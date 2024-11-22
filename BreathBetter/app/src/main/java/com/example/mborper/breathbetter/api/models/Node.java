package com.example.mborper.breathbetter.api.models;

import java.util.Date;

/**
 * Represents a Node in the BreathBetter application, storing information about
 * the user's associated node, including its status and the date of the last status update.
 * Provides getter and setter methods for accessing and modifying node details.
 *
 * This model helps track node-specific details, allowing the user to see information
 * about their current node association within the app.
 *
 * @author Juan Diaz
 * @since 2024-11-14
 */
public class Node {
    private int id;
    private Integer userId;
    private String status;
    private Date lastStatusUpdate;

    /**
     * Gets the unique identifier for this node.
     *
     * @return the node ID.
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the unique identifier for this node.
     *
     * @param id the ID to set for the node.
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Gets the user ID associated with this node.
     *
     * @return the user ID if associated, otherwise null.
     */
    public Integer getUserId() {
        return userId;
    }

    /**
     * Sets the user ID associated with this node.
     *
     * @param userId the user ID to associate with the node.
     */
    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    /**
     * Gets the current status of this node.
     *
     * @return the status of the node, e.g., "ACTIVE", "INACTIVE", or "UNLINKED".
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the current status of this node.
     *
     * @param status the status to set for the node, e.g., "ACTIVE", "INACTIVE", or "UNLINKED".
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the date and time of the last status update for this node.
     *
     * @return the last status update timestamp.
     */
    public Date getLastStatusUpdate() {
        return lastStatusUpdate;
    }

    /**
     * Sets the date and time of the last status update for this node.
     *
     * @param lastStatusUpdate the timestamp to set for the last status update.
     */
    public void setLastStatusUpdate(Date lastStatusUpdate) {
        this.lastStatusUpdate = lastStatusUpdate;
    }
}

