package chattcp;
import java.io.Serializable;
import java.util.*;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id; // Unique ID
    private String sender;
    private String content;
    private long timestamp;
    private String type; // "text", "file", "image", "audio"

    // Reply/Quote
    private String replyToId;
    private String replyToContent;

    // Reactions
    private Map<String, String> reactions; // username -> emoji

    // Read receipts
    private Set<String> readBy;

    // File transfer
    private byte[] fileData;
    private String fileName;
    private String fileType;

    public Message(String id, String sender, String content, String type) {
        this.id = id;
        this.sender = sender;
        this.content = content;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.reactions = new HashMap<>();
        this.readBy = new HashSet<>();
    }

    // Getters and Setters
    public String getId() { return id; }
    public String getSender() { return sender; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }
    public String getType() { return type; }

    public String getReplyToId() { return replyToId; }
    public void setReplyToId(String replyToId) { this.replyToId = replyToId; }

    public String getReplyToContent() { return replyToContent; }
    public void setReplyToContent(String replyToContent) { this.replyToContent = replyToContent; }

    public Map<String, String> getReactions() { return reactions; }
    public void addReaction(String username, String emoji) { reactions.put(username, emoji); }
    public void removeReaction(String username) { reactions.remove(username); }

    public Set<String> getReadBy() { return readBy; }
    public void markReadBy(String username) { readBy.add(username); }

    public byte[] getFileData() { return fileData; }
    public void setFileData(byte[] fileData) { this.fileData = fileData; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public String getReactionsString() {
        if (reactions.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : reactions.entrySet()) {
            sb.append(entry.getValue());
        }
        return sb.toString();
    }

    public String getReadReceiptsString() {
        if (readBy.isEmpty()) return "";
        return "✓✓ " + readBy.size();
    }
}