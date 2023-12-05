/*
  Copyright 2008 Google Inc.
  
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  
       http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.

  Modified by Curtis Gedak 2015, 2016
*/
package net.sourceforge.solitaire_cg;

import android.os.Bundle;
import java.util.Stack;


public abstract class Rules {

  public static final int FORTYTHIEVES = 1;
  public static final int FREECELL     = 2;
  public static final int GOLF         = 3;
  public static final int KLONDIKE     = 4;
  public static final int SPIDER       = 5;
  public static final int TRIPEAKS     = 6;

  public static final int EVENT_INVALID = -1;
  public static final int EVENT_DEAL = 1;
  public static final int EVENT_STACK_ADD = 2;
  public static final int EVENT_FLING = 3;
  public static final int EVENT_SMART_MOVE = 4;
  public static final int EVENT_DEAL_NEXT = 5;
  public static final int EVENT_STACK_TAP = 6;

  public static final int AUTO_MOVE_ALWAYS = 2;
  public static final int AUTO_MOVE_FLING_ONLY = 1;
  public static final int AUTO_MOVE_NEVER = 0;

  private int mType;
  protected SolitaireView mView;
  protected Stack<Move> mMoveHistory;
  protected AnimateCard mAnimateCard; 
  protected boolean mIgnoreEvents;
  protected EventPoster mEventPoster;
  protected boolean mWon;


  // Anchors
  protected CardAnchor[] mCardAnchor;
  protected int mCardAnchorCount;

  protected Deck mDeck;
  protected int mCardCount;

  // Automove
  protected int mAutoMoveLevel;
  protected boolean mWasFling;

  public boolean HasWon() { return mWon; }
  public int GetType() { return mType; }
  public int GetCardCount() { return mCardCount; }
  public CardAnchor[] GetAnchorArray() { return mCardAnchor; }
  public void SetType(int type) { mType = type; }
  public void SetView(SolitaireView view) { mView = view; }
  public void SetMoveHistory(Stack<Move> moveHistory) { mMoveHistory = moveHistory; }
  public void SetAnimateCard(AnimateCard animateCard) { mAnimateCard = animateCard; }
  public void SetIgnoreEvents(boolean ignore) { mIgnoreEvents = ignore; }
  public void SetEventPoster(EventPoster ep) { mEventPoster = ep; }
  public boolean GetIgnoreEvents() { return mIgnoreEvents; }
  public int GetRulesExtra() { return 0; }
  public String GetGameTypeString() { return ""; }
  public String GetPrettyGameTypeString() { return ""; }
  public boolean HasScore() { return false; }
  public boolean HasString() { return false; }
  public String GetString() { return ""; }
  public void SetCarryOverScore(int score) {}
  public int GetScore() { return 0; }
  public void AddDealCount() {}
  public void MarkBlockedCards() {}

  // Free Spaces are used in games like Forty Thieves and Freecell
  public int CountFreeSpaces() { return 0; }
  public int CountFreeSpacesMin() { return CountFreeSpaces(); }

  protected void SignalWin() { mWon = true; mView.MarkWin(); mView.DisplayWin(); }

  abstract public void Init(Bundle map);
  public void EventAlert(int event) { if (!mIgnoreEvents) { mEventPoster.PostEvent(event); mView.Refresh(); } }
  public void EventAlert(int event, CardAnchor anchor) { if (!mIgnoreEvents) { mEventPoster.PostEvent(event, anchor);  mView.Refresh();} }
  public void EventAlert(int event, CardAnchor anchor, Card card) { if (!mIgnoreEvents) { mEventPoster.PostEvent(event, anchor, card);  mView.Refresh();} }
  public void ClearEvent() { mEventPoster.ClearEvent(); }
  abstract public void EventProcess(int event, CardAnchor anchor);
  abstract public void EventProcess(int event, CardAnchor anchor, Card card);
  abstract public void EventProcess(int event);
  abstract public void Resize(int width, int height);
  public boolean Fling(MoveCard moveCard) { moveCard.Release(); return false; }
  public void HandleEvents() { 
    while (!mIgnoreEvents && mEventPoster.HasEvent()) {
      mEventPoster.HandleEvent();
    }
  }

  public void RefreshOptions() {
    mAutoMoveLevel = Integer.parseInt(mView.GetSettings().getString("AutoMoveLevel", "1" /* default - fling only */));
    mWasFling = false;
  }

  public static Rules CreateRules(int type, Bundle map, SolitaireView view,
                                  Stack<Move> moveHistory, AnimateCard animate) {
    Rules ret = null;
    switch (type) {
      case FORTYTHIEVES:
        ret = new RulesFortyThieves();
        break;
      case FREECELL:
        ret = new RulesFreecell();
        break;
      case GOLF:
        ret = new RulesGolf();
        break;
      case KLONDIKE:
        ret = new RulesKlondike();
        break;
      case SPIDER:
        ret = new RulesSpider();
        break;
      case TRIPEAKS:
        ret = new RulesTriPeaks();
        break;
    }

    if (ret != null) {
      ret.mWon = false;
      ret.SetType(type);
      ret.SetView(view);
      ret.SetMoveHistory(moveHistory);
      ret.SetAnimateCard(animate);
      ret.SetEventPoster(new EventPoster(ret));
      ret.RefreshOptions();
      ret.Init(map);
    }
    return ret;
  }

  public void FinishDeal() {};  // Used when multi-stack deal interrupted
}



class EventPoster {
  private int mEvent;
  private CardAnchor mCardAnchor;
  private Card mCard;
  private Rules mRules;

  public EventPoster(Rules rules) {
    mRules = rules;
    mEvent = -1;
    mCardAnchor = null;
    mCard = null;
  }

  public void PostEvent(int event) {
    PostEvent(event, null, null);
  }

  public void PostEvent(int event, CardAnchor anchor) {
    PostEvent(event, anchor, null);
  }

  public void PostEvent(int event, CardAnchor anchor, Card card) {
    mEvent = event;
    mCardAnchor = anchor;
    mCard = card;
  }


  public void ClearEvent() {
    mEvent = Rules.EVENT_INVALID;
    mCardAnchor = null;
    mCard = null;
  }

  public boolean HasEvent() {
    return mEvent != Rules.EVENT_INVALID;
  }

  public void HandleEvent() {
    if (HasEvent()) {
      int event = mEvent;
      CardAnchor cardAnchor = mCardAnchor;
      Card card = mCard;
      ClearEvent();
      if (cardAnchor != null && card != null) {
        mRules.EventProcess(event, cardAnchor, card);
      } else if (cardAnchor != null) {
        mRules.EventProcess(event, cardAnchor);
      } else {
        mRules.EventProcess(event);
      }
    }
  }
}


