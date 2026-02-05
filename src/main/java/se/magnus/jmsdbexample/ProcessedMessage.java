package se.magnus.jmsdbexample;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "processed_messages")
public class ProcessedMessage {

    @Id
    private String messageId;

    private String content;

    public ProcessedMessage() {}

    public ProcessedMessage(String messageId, String content) {
        this.messageId = messageId;
        this.content = content;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
