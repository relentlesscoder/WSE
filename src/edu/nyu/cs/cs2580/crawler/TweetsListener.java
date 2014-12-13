package edu.nyu.cs.cs2580.crawler;

import edu.nyu.cs.cs2580.utils.WriteFile;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;

/**
 * Created by tanis on 10/9/14.
 */
public class TweetsListener implements StatusListener {

    private int counts;

    public TweetsListener(){
        counts = 0;
    }
    @Override
    public void onStatus(Status status) {
        this.counts ++;
        StringBuilder sb = new StringBuilder();
        sb.append(counts).append('\t');
        sb.append("@").append(status.getUser().getScreenName())
                .append('\t')
                .append(status.getCreatedAt().toString())
                .append(" - ").append(status.getText());
        sb.append('\n');
        System.out.println(sb.toString());

        WriteFile.WriteToFile(sb.toString(), "Tweets", true);
    }

    @Override
    public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
//                System.out.println("Got a status deletion notice id:" + statusDeletionNotice.getStatusId());
    }

    @Override
    public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
//                System.out.println("Got track limitation notice:" + numberOfLimitedStatuses);
    }

    @Override
    public void onScrubGeo(long userId, long upToStatusId) {
//                System.out.println("Got scrub_geo event userId:" + userId + " upToStatusId:" + upToStatusId);
    }

    @Override
    public void onStallWarning(StallWarning warning) {
//                System.out.println("Got stall warning:" + warning);
    }

    @Override
    public void onException(Exception ex) {
//                ex.printStackTrace();
    }

    public int GetCounts(){
        return counts;
    }
}
