/*
  Copyright 2015, 2016 Curtis Gedak

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/
package net.sourceforge.solitaire_cg;

import android.os.Bundle;


class RulesGolf extends Rules {

  private boolean mWrapCards;

  @Override
  public void Init(Bundle map) {
    mIgnoreEvents = true;
    mWrapCards = mView.GetSettings().getBoolean("GolfWrapCards", false);

    // Nine total anchors for golf - Stock plus Waste plus 7 tableaus
    mCardCount = 52;
    mCardAnchorCount = 9;
    mCardAnchor = new CardAnchor[mCardAnchorCount];

    // Top dealt from anchor and waste/sink anchor
    mCardAnchor[0] = CardAnchor.CreateAnchor(CardAnchor.DEAL_FROM, 0, this);
    if (!mWrapCards) {
      mCardAnchor[1] = CardAnchor.CreateAnchor(CardAnchor.GOLF_WASTE, 1, this);
    } else {
      mCardAnchor[1] = CardAnchor.CreateAnchor(CardAnchor.GOLF_WRAPCARDS_WASTE, 1, this);
    }

    // Middle anchor stacks
    for (int i = 0; i < 7; i++) {
      mCardAnchor[i+2] = CardAnchor.CreateAnchor(CardAnchor.GOLF_STACK, i+2, this);
    }

    if (map != null) {
      // Do some assertions, default to a new game if we find an invalid state
      if (map.getInt("cardAnchorCount") == 9 &&
          map.getInt("cardCount") == 52) {
        int[] cardCount = map.getIntArray("anchorCardCount");
        int[] hiddenCount = map.getIntArray("anchorHiddenCount");
        int[] value = map.getIntArray("value");
        int[] suit = map.getIntArray("suit");
        int cardIdx = 0;

        for (int i = 0; i < 9; i++) {
          for (int j = 0; j < cardCount[i]; j++, cardIdx++) {
            Card card = new Card(value[cardIdx], suit[cardIdx]);
            mCardAnchor[i].AddCard(card);
          }
          mCardAnchor[i].SetHiddenCount(hiddenCount[i]);
        }

        mIgnoreEvents = false;
        // Return here so an invalid save state will result in a new game
        return;
      }
    }

    mDeck = new Deck(1);
    // Deal out initial cards
    for (int i = 0; i < 7; i++) {
      for (int j = 0; j < 5 ; j++) {
        mCardAnchor[i+2].AddCard(mDeck.PopCard());
      }
    }

    while (!mDeck.Empty()) {
      mCardAnchor[0].AddCard(mDeck.PopCard());
    }

    mIgnoreEvents = false;
  }

  @Override
  public void Resize(int width, int height) {
    int rem = width - Card.WIDTH*7;
    int maxHeight = height - (20 + Card.HEIGHT);
    rem /= 8;
    for (int i = 0; i < 7; i++) {
      mCardAnchor[i+2].SetPosition(rem + i * (rem+Card.WIDTH), 20 + Card.HEIGHT);
      mCardAnchor[i+2].SetMaxHeight(maxHeight);
    }

    // Position deck and waste right to left
    for (int i = 0; i < 2; i++) {
      mCardAnchor[i].SetPosition(rem + (6 - i) * (rem + Card.WIDTH), 10);
    }

    // Setup edge cards (Touch sensor loses sensitivity towards the edge).
    mCardAnchor[0].SetRightEdge(width);
    mCardAnchor[2].SetLeftEdge(0);
    mCardAnchor[8].SetRightEdge(width);
    for (int i = 0; i < 7; i++) {
      mCardAnchor[i+2].SetBottom(height);
    }
  }

  @Override
  public void EventProcess(int event, CardAnchor anchor) {
    if (mIgnoreEvents) {
      return;
    }
    if (event == EVENT_DEAL) {
      if (mCardAnchor[0].GetCount() > 0) {
        mCardAnchor[1].AddCard(mCardAnchor[0].PopCard());
        if (mCardAnchor[0].GetCount() == 0) {
          mCardAnchor[0].SetDone(true);
        }
        mMoveHistory.push(new Move(0, 1, 1, true, false));
      }
    } else if (event == EVENT_STACK_TAP) {
      TryToSink(anchor);
    } else if (event == EVENT_STACK_ADD) {
      if ((mCardAnchor[0].GetCount() + mCardAnchor[1].GetCount()) == 52) {
        SignalWin();
      } else {
        if (mAutoMoveLevel == AUTO_MOVE_ALWAYS ||
            (mAutoMoveLevel == AUTO_MOVE_FLING_ONLY && mWasFling)) {
          EventAlert(EVENT_SMART_MOVE);
        } else {
          mView.StopAnimating();
          mWasFling = false;
        }
      }
    }
  }

  @Override
  public void EventProcess(int event, CardAnchor anchor, Card card) {
    if (mIgnoreEvents) {
      anchor.AddCard(card);
      return;
    }
    if (event == EVENT_FLING) {
      mWasFling = true;
      if (!TryToSinkCard(anchor, card)) {
        anchor.AddCard(card);
        mWasFling = false;
      }
    } else {
      anchor.AddCard(card);
    }
  }

  @Override
  public void EventProcess(int event) {
    if (mIgnoreEvents) {
      return;
    }
    if (event == EVENT_SMART_MOVE) {
      int candidate = -1;
      int numCandidates = 0;
      for (int i = 0; i < 7; i++) {
        // Check for valid sink card candidate
        if (mCardAnchor[i+2].GetCount() > 0) {
          Card card = mCardAnchor[i+2].PopCard();
          if (mCardAnchor[1].DropSingleCard(card)) {
            candidate = i;
            numCandidates++;
          }
          mCardAnchor[i+2].AddCard(card);
        }
      }
      // Sink card only if one and only one valid card candidate
      if (numCandidates == 1) {
        TryToSink(mCardAnchor[candidate+2]);
      } else {
        mWasFling = false;
        mView.StopAnimating();
      }
    }
  }

  @Override
  public boolean Fling(MoveCard moveCard) {
    if (moveCard.GetCount() == 1) {
      CardAnchor anchor = moveCard.GetAnchor();
      Card card = moveCard.DumpCards(false)[0];
      if (mCardAnchor[1].DropSingleCard(card)) {
        EventAlert(EVENT_FLING, anchor, card);
        return true;
      }
      anchor.AddCard(card);
    } else {
      moveCard.Release();
    }
    return false;
  }

  private boolean TryToSink(CardAnchor anchor) {
    Card card = anchor.PopCard();
    boolean ret = TryToSinkCard(anchor, card);
    if (!ret) {
      anchor.AddCard(card);
    }
    return ret;
  }

  private boolean TryToSinkCard(CardAnchor anchor, Card card) {
    if (mCardAnchor[1].DropSingleCard(card)) {
      mMoveHistory.push(new Move(anchor.GetNumber(), 1, 1, false, anchor.UnhideTopCard()));
      mAnimateCard.MoveCard(card, mCardAnchor[1]);
      return true;
    }

    return false;
  }

  @Override
  public String GetGameTypeString() {
    if (!mWrapCards) {
      return "Golf";
    } else {
      return "GolfWrapCards";
    }
  }

  @Override
  public String GetPrettyGameTypeString() {
    if (!mWrapCards) {
      return mView.GetContext().getResources().getString(R.string.menu_golf);
    } else {
      return mView.GetContext().getResources().getString(R.string.menu_golf_wrapcards);
    }
  }

}
