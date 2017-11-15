package com.gmail.jorgegilcavazos.ballislife.features.highlights;

import com.gmail.jorgegilcavazos.ballislife.features.model.Highlight;
import com.gmail.jorgegilcavazos.ballislife.features.model.HighlightViewType;

import java.util.List;

import io.reactivex.Observable;

public interface HighlightsView {

    void setLoadingIndicator(boolean active);

    void hideHighlights();

    void showHighlights(List<Highlight> highlights, boolean clear);

    void showNoHighlightsAvailable();

    void showNoNetAvailable();

    void showErrorLoadingHighlights(int code);

    void openStreamable(String shortcode);

    void showErrorOpeningStreamable();

    void openYoutubeVideo(String videoId);

    void showErrorOpeningYoutube();

    void showUnknownSourceError();

    void resetScrollState();

    void shareHighlight(Highlight highlight);

    void changeViewType(HighlightViewType viewType);

    void hideSnackbar();

    void onSubmissionClick(Highlight highlight);

    Sorting getSorting();

    Observable<Object> explorePremiumClicks();

    Observable<Object> premiumGotItClicks();

    Observable<Object> shortcutClicks();

    Observable<Object> shortcutGotItClicks();

    void openPremiumActivity();

    void openShortcutActivity();

    void dismissSwishCard();

    void dismissShortcutCard();
}
