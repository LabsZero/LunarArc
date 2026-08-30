package org.bukkit.craftbukkit.conversations;

import java.util.LinkedList;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ManuallyAbandonedConversationCanceller;

public final class ConversationTracker {
    private LinkedList<Conversation> conversationQueue = new LinkedList<>();

    public synchronized boolean beginConversation(Conversation conversation) {
        if (!this.conversationQueue.contains(conversation)) {
            this.conversationQueue.addLast(conversation);
            if (this.conversationQueue.getFirst() == conversation) {
                conversation.begin();
                conversation.outputNextPrompt();
            }
        }
        return true;
    }

    public synchronized void abandonConversation(Conversation conversation, ConversationAbandonedEvent details) {
        if (this.conversationQueue.isEmpty()) {
            return;
        }
        if (this.conversationQueue.getFirst() == conversation) {
            conversation.abandon(details);
        }
        if (this.conversationQueue.remove(conversation) && !this.conversationQueue.isEmpty()) {
            this.conversationQueue.getFirst().outputNextPrompt();
        }
    }

    public synchronized void abandonAllConversations() {
        LinkedList<Conversation> oldQueue = this.conversationQueue;
        this.conversationQueue = new LinkedList<>();
        for (Conversation conversation : oldQueue) {
            try {
                conversation.abandon(new ConversationAbandonedEvent(
                        conversation, new ManuallyAbandonedConversationCanceller()));
            } catch (Throwable throwable) {
                Bukkit.getLogger().log(Level.SEVERE, "Unexpected exception while abandoning a conversation", throwable);
            }
        }
    }

    public synchronized void acceptConversationInput(String input) {
        if (!this.conversationQueue.isEmpty()) {
            Conversation conversation = this.conversationQueue.getFirst();
            try {
                conversation.acceptInput(input);
            } catch (Throwable throwable) {
                conversation.getContext().getPlugin().getLogger().log(
                        Level.WARNING,
                        "Plugin " + conversation.getContext().getPlugin().getDescription().getFullName()
                                + " generated an exception whilst handling conversation input",
                        throwable);
            }
        }
    }

    public synchronized boolean isConversing() {
        return !this.conversationQueue.isEmpty();
    }

    public synchronized boolean isConversingModaly() {
        return !this.conversationQueue.isEmpty() && this.conversationQueue.getFirst().isModal();
    }
}
