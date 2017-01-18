package data;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.avaje.ebean.RawSqlBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import helper.Counter;
import helper.DatetimeHelper;
import helper.JsonHelper;
import helper.TweetWrapper;
import models.HashtagCounter;
import twitter4j.HashtagEntity;
import twitter4j.Status;

import java.util.Date;
import java.util.List;

public class RecentHashtags {
    private static Counter counter = new Counter(4);

    public static JsonNode getRecentHashtags(int entityId, Date startTime, Date endTime, int maxRows){
        String sql = "select  hashtag, sum(count) as count from hashtag_counter group by hashtag";
        Query<HashtagCounter> query = Ebean.find(HashtagCounter.class);
        query.setRawSql(RawSqlBuilder.parse(sql).create()).where().eq("entity_id", entityId)
                .le("created_at", endTime)
                .ge("created_at", startTime)
                .setMaxRows(maxRows)
                .order().desc("count")
                .findList();

        return JsonHelper.jsonify(query.findList());
    }

    public static void hashtags(TweetWrapper tweetWrapper, List<Integer> presentEntities) {
        if (counter.incrCounter() != 0) return;

        if (!tweetWrapper.status.isRetweet()){
            Status status = tweetWrapper.status;
            HashtagEntity[] hashtagEntities = status.getHashtagEntities();
            Date trimmedDate = DatetimeHelper.getDateUptoHours(status.getCreatedAt());
            for (HashtagEntity hashtagEntity: hashtagEntities){
                String hashtag = hashtagEntity.getText();
                for (Integer entityId: presentEntities){
                    HashtagCounter hashtagCounter = Ebean.find(HashtagCounter.class).where()
                            .eq("hashtag", hashtag)
                            .eq("entity_id", entityId)
                            .eq("created_at", trimmedDate)
                            .findUnique();
                    if (hashtagCounter == null){
                        hashtagCounter = new HashtagCounter();
                        hashtagCounter.count = 1;
                        hashtagCounter.createdAt = trimmedDate;
                        hashtagCounter.hashtag = hashtag;
                        hashtagCounter.entityId = entityId;
                    }
                    else {
                        hashtagCounter.setCount(hashtagCounter.count+1);
                    }
                    Ebean.save(hashtagCounter);
                }
            }
        }
    }
}