package com.atmko.onmywatch.models;

import java.util.List;

public class Season extends ScheduledMedia{
    String parentMediaId;
    public int seasonNumber;
    private List<Episode> episodes;
    private boolean hasEnded;
    public int episodesAired;
    public long timestamp;
    public boolean isBundled;

    public Season(String parentMediaId, int seasonNumber, String airDate) {
        this.parentMediaId = parentMediaId;
        this.seasonNumber = seasonNumber;
        try {
            setAirDate(airDate);
        } catch (DateFormatException e) {
            e.printStackTrace();
        }

        if (getBestLocalAirDate() != null) {
            timestamp = getBestLocalAirDate().getTime();

        } else {
            timestamp = Long.MAX_VALUE;
        }
    }

    public List<Episode> getEpisodes() {
        return episodes;
    }

    public void setEpisodes(List<Episode> episodes) {
        this.episodes = episodes;
        setIsBundled();
    }

    public void setEpisodesAired(int episodesAired) {
        this.episodesAired = episodesAired;
    }

    public boolean hasEnded() {
        return episodesAired >= episodes.size();
    }

    public void overrideEpisode(Episode episodeOverride) {
        for (int i = 0; i < episodes.size(); i++) {
            if (episodeOverride.seasonNumber == episodes.get(i).seasonNumber
                    && episodeOverride.episodeNumber == episodes.get(i).episodeNumber) {
                episodes.remove(i);
                episodes.add(i, episodeOverride);
                break;
            }
        }
    }

    public Episode getEpisode(int episodeNumber) {
        if (episodeNumber > 0 && episodeNumber <= episodes.size()) {
            return episodes.get(episodeNumber - 1);

        } else {
            return null;
        }
    }

    public Episode getNextEpisodeInSeason() {
        int nextEpisodeNumber = episodesAired + 1;

        if (nextEpisodeNumber <= episodes.size()) {
            return getEpisode(nextEpisodeNumber);

        } else {
            return null;
        }
    }

    private void setIsBundled() {
        boolean isBundled = true;

        for (int i = 0; i < episodes.size() - 1; i++) {
            String currentEpisodeAirDate = episodes.get(i).getBestAvailableDateString();
            String nextEpisodeAirDate = episodes.get(i + 1).getBestAvailableDateString();

            if (currentEpisodeAirDate != null && nextEpisodeAirDate != null) {
                isBundled = currentEpisodeAirDate.equals(nextEpisodeAirDate);
            }
        }

        this.isBundled = isBundled;
    }
 }