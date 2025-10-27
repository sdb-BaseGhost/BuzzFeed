package org.sdb.buzzfeed.service;

import org.sdb.buzzfeed.entity.Content;
import org.sdb.buzzfeed.entity.Feed;

import java.util.List;

public interface FeedService {
    List<Content> getFeed(Feed feed);
}
