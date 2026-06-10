package org.sdb.buzzfeed.service;

import org.sdb.buzzfeed.entity.Feed;
import org.sdb.buzzfeed.entity.vo.FeedItemVO;

import java.util.List;

public interface FeedService {
    List<FeedItemVO> getFeed(Feed feed);
}
