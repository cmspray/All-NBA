package com.gmail.jorgegilcavazos.ballislife.features.highlights;

import android.support.annotation.NonNull;

import com.gmail.jorgegilcavazos.ballislife.base.BasePresenter;
import com.gmail.jorgegilcavazos.ballislife.data.local.LocalRepository;
import com.gmail.jorgegilcavazos.ballislife.data.repository.highlights.HighlightsRepository;
import com.gmail.jorgegilcavazos.ballislife.features.model.Highlight;
import com.gmail.jorgegilcavazos.ballislife.features.model.HighlightViewType;
import com.gmail.jorgegilcavazos.ballislife.features.model.SwishCard;
import com.gmail.jorgegilcavazos.ballislife.util.Constants;
import com.gmail.jorgegilcavazos.ballislife.util.ErrorHandler;
import com.gmail.jorgegilcavazos.ballislife.util.NetworkUtils;
import com.gmail.jorgegilcavazos.ballislife.util.Utilities;
import com.gmail.jorgegilcavazos.ballislife.util.schedulers.BaseSchedulerProvider;

import java.util.List;
import java.util.NoSuchElementException;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.observers.DisposableSingleObserver;

public class HighlightsPresenter extends BasePresenter<HighlightsView> {

    private HighlightsRepository highlightsRepository;
    private LocalRepository localRepository;
    private BaseSchedulerProvider schedulerProvider;
    private CompositeDisposable disposables;
    private NetworkUtils networkUtils;
    private ErrorHandler errorHandler;

    @Inject
    public HighlightsPresenter(
            HighlightsRepository highlightsRepository,
            LocalRepository localRepository,
            BaseSchedulerProvider schedulerProvider,
            NetworkUtils networkUtils,
            ErrorHandler errorHandler) {
        this.highlightsRepository = highlightsRepository;
        this.localRepository = localRepository;
        this.schedulerProvider = schedulerProvider;
        this.networkUtils = networkUtils;
        this.errorHandler = errorHandler;

        disposables = new CompositeDisposable();
    }

    @Override
    public void attachView(@NonNull HighlightsView view) {
        super.attachView(view);

        disposables.add(view.explorePremiumClicks()
                .subscribe(o -> {
                    localRepository.markSwishCardSeen(SwishCard.HIGHLIGHT_SORTING);
                    view.dismissSwishCard();
                    view.openPremiumActivity();
                }));

        disposables.add(view.premiumGotItClicks()
                .subscribe(o -> {
                    localRepository.markSwishCardSeen(SwishCard.HIGHLIGHT_SORTING);
                    view.dismissSwishCard();
                }));

        disposables.add(view.shortcutClicks()
                .subscribe(o -> {
                    localRepository.markShortcutSeen(Constants.HIGHLIGHT_SHORTCUT_SEEN);
                    view.dismissShortcutCard();
                    view.openPremiumActivity();
                }));

        disposables.add(view.shortcutGotItClicks()
                .subscribe(o -> {
                    localRepository.markShortcutSeen(Constants.HIGHLIGHT_SHORTCUT_SEEN);
                    view.dismissShortcutCard();
                }));

    }

    @Override
    public void detachView() {
        disposables.clear();
        view.hideSnackbar();
        super.detachView();
    }

    public void setItemsToLoad(int itemsToLoad) {
        highlightsRepository.setItemsToLoad(itemsToLoad);
    }

    public void loadFirstAvailable() {
        List<Highlight> highlights = highlightsRepository.getCachedHighlights();
        if (highlights.isEmpty() || highlightsRepository.getSorting() != view.getSorting()) {
            loadHighlights(true);
        } else {
            view.showHighlights(highlights, true);
        }
    }

    public void loadHighlights(final boolean reset) {
        if (reset) {
            view.resetScrollState();
            view.setLoadingIndicator(true);
            view.hideHighlights();
            highlightsRepository.reset(view.getSorting());
        }
        disposables.add(highlightsRepository.next()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeWith(new DisposableSingleObserver<List<Highlight>>() {
                    @Override
                    public void onSuccess(List<Highlight> highlights) {
                        if (highlights.isEmpty()) {
                            view.showNoHighlightsAvailable();
                        } else {
                            view.showHighlights(highlights, reset);
                        }

                        if (reset) {
                            view.setLoadingIndicator(false);
                        }
                        view.hideSnackbar();
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (!networkUtils.isNetworkAvailable()) {
                            view.showNoNetAvailable();
                        } else if (e instanceof NoSuchElementException) {
                            view.showNoHighlightsAvailable();
                        } else {
                            view.showErrorLoadingHighlights(errorHandler.handleError(e));
                        }

                        if (reset) {
                            view.setLoadingIndicator(false);
                        }
                    }
                })
        );
    }

    public void subscribeToHighlightsClick(Observable<Highlight> highlightsClick) {
        disposables.add(highlightsClick
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeWith(new DisposableObserver<Highlight>() {
                    @Override
                    public void onNext(Highlight hl) {
                        if (hl.getUrl().contains("streamable")) {
                            String shortCode = Utilities.getStreamableShortcodeFromUrl(hl.getUrl());
                            if (shortCode != null) {
                                view.openStreamable(shortCode);
                            } else {
                                view.showErrorOpeningStreamable();
                            }
                        } else if (hl.getUrl().contains("youtube") || hl.getUrl().contains("youtu" +
                                ".be")) {
                            String videoId = Utilities.getYoutubeVideoIdFromUrl(hl.getUrl());
                            if (videoId == null) {
                                view.showErrorOpeningYoutube();
                            } else {
                                view.openYoutubeVideo(videoId);
                            }
                        } else {
                            view.showUnknownSourceError();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        view.showErrorOpeningStreamable();
                    }

                    @Override
                    public void onComplete() {

                    }
                })
        );
    }

    public void subscribeToHighlightsShare(Observable<Highlight> highlightShare) {
        disposables.add(highlightShare
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeWith(new DisposableObserver<Highlight>() {
                    @Override
                    public void onNext(Highlight highlight) {
                        view.shareHighlight(highlight);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    public void onComplete() {

                    }
                })
        );
    }

    public void subscribeToSubmissionClick(Observable<Highlight> highlightShare) {
        disposables.add(highlightShare
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeWith(new DisposableObserver<Highlight>() {
                    @Override
                    public void onNext(Highlight highlight) {
                        view.onSubmissionClick(highlight);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    public void onComplete() {

                    }
                })
        );
    }

    public void onViewTypeSelected(HighlightViewType viewType) {
        localRepository.saveFavoriteHighlightViewType(viewType);
        view.changeViewType(viewType);
    }

}
