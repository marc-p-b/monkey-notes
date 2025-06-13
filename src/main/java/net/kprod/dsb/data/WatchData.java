package net.kprod.dsb.data;

import com.google.api.services.drive.model.Channel;

import java.util.concurrent.ScheduledFuture;

public class WatchData {
    private String channelId;
    private String username;
    private String lastPageToken = null;
    private Channel channel;
    private ScheduledFuture<?> futureFlush;
    private boolean watchChanges = false;

    public String getChannelId() {
        return channelId;
    }

    public WatchData setChannelId(String channelId) {
        this.channelId = channelId;
        return this;
    }

    public String getLastPageToken() {
        return lastPageToken;
    }

    public WatchData setLastPageToken(String lastPageToken) {
        this.lastPageToken = lastPageToken;
        return this;
    }

    public Channel getChannel() {
        return channel;
    }

    public WatchData setChannel(Channel channel) {
        this.channel = channel;
        return this;
    }

    public ScheduledFuture<?> getFutureFlush() {
        return futureFlush;
    }

    public WatchData setFutureFlush(ScheduledFuture<?> futureFlush) {
        this.futureFlush = futureFlush;
        return this;
    }

    public boolean isWatchChanges() {
        return watchChanges;
    }

    public WatchData setWatchChanges(boolean watchChanges) {
        this.watchChanges = watchChanges;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public WatchData setUsername(String username) {
        this.username = username;
        return this;
    }
}