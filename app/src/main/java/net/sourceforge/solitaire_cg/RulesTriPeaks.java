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


class RulesTriPeaks extends Rules {

  private boolean mWrapCards;

  @Override
  public void Init(Bundle map) {
    mIgnoreEvents = true;
    mWrapCards = mView.GetSettings().getBoolean("GolfWrapCards", false);

    // Thirty anchors for TriPeaks - Stock plus Waste plus 28 RowStack
    mCardCount = 52;
    mCardAnchorCount = 30;
    mCardAnchor = new CardAnchor[mCardAnchorCount];

    // Lower TriPeaks Row Stacks
    for (int i = 0; i < 28; i++) {
      mCardAnchor[i] = CardAnchor.CreateAnchor(CardAnchor.GOLF_STACK, i, this);
    }

    // Top dealt from deck anchor to waste/sink anchor
    mCardAnchor[28] = CardAnchor.CreateAnchor(CardAnchor.DEAL_FROM, 28, this);
    if (!mWrapCards) {
      mCardAnchor[29] = CardAnchor.CreateAnchor(CardAnchor.GOLF_WASTE, 29, this);
    } else {
      mCardAnchor[29] = CardAnchor.CreateAnchor(CardAnchor.GOLF_WRAPCARDS_WASTE, 29, this);
    }

    if (map != null) {
      // Do some assertions, default to a new game if we find an invalid state
      if (map.getInt("cardAnchorCount") == 30 &&
          map.getInt("cardCount") == 52) {
        int[] cardCount = map.getIntArray("anchorCardCount");
        int[] hiddenCount = map.getIntArray("anchorHiddenCount");
        int[] value = map.getIntArray("value");
        int[] suit = map.getIntArray("suit");
        int cardIdx = 0;

        for (int i = 0; i < 30; i++) {
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
    for (int i = 0; i < 28; i++) {
      mCardAnchor[i].AddCard(mDeck.PopCard());
    }

    while (!mDeck.Empty()) {
      mCardAnchor[28].AddCard(mDeck.PopCard());
    }

    MarkBlockedCards();

    mIgnoreEvents = false;
  }

  protected boolean IsBlocked(int idx) {
    int[] step = {3, 4, 5, 6, 6, 7, 7, 8, 8, 9, 9, 9, 9, 9, 9, 9, 9, 9};
    int i = idx;
    while (i < 18) {
      i = i + step[i];
      for (int j = 0; j < 2; j++) {
        if (mCardAnchor[i + j].GetCount() >= 1) {
          return true;
        }
      }
    }
    return false;
  }

  public void MarkBlockedCards() {
    for (int i = 0; i < 18; i++) {
      if (IsBlocked(i)) {
        // Use HiddenCount to indicate blocked cards
        mCardAnchor[i].SetHiddenCount(1);
      } else {
        mCardAnchor[i].SetHiddenCount(0);
      }
    }
  }

  @Override
  public void Resize(int width, int height) {
    int rem = (width - (Card.WIDTH * 10)) / 11;
    int maxHeight = height - (20 + Card.HEIGHT);
    int vSpacing = Math.min( maxHeight / 5 - 20, Card.HEIGHT * 3 / 5);
    int x = -1;
    int y = -1;
    // Position three peaks cards
    x = rem*5/2 + Card.WIDTH*3/2;
    y = 20 + Card.HEIGHT;
    for (int i = 0; i < 3; i++) {
      mCardAnchor[i].SetPosition(x, y);
      x += rem*3 + Card.WIDTH*3;
    }
    // Position next six cards
    x = rem*2 + Card.WIDTH;
    y += vSpacing;
    for (int i = 3; i < 9; i++) {
      mCardAnchor[i].SetPosition(x, y);
      x += rem + Card.WIDTH;
      i++;  // Extra increment of loop counter
      mCardAnchor[i].SetPosition(x, y);
      x += rem*2 + Card.WIDTH*2;
    }
    // Position next nine cards
    x = rem*3/2 + Card.WIDTH/2;
    y += vSpacing;
    for (int i = 9; i < 18; i++) {
      mCardAnchor[i].SetPosition(x, y);
      x += rem + Card.WIDTH;
    }
    // Position last ten cards
    x = rem;
    y += vSpacing;
    for (int i = 18; i < 28; i++) {
      mCardAnchor[i].SetPosition(x, y);
      x += rem + Card.WIDTH;
    }

    // Position deck and waste right to left
    for (int i = 28; i < 30; i++) {
      mCardAnchor[i].SetPosition(rem + (37 - i) * (rem + Card.WIDTH), 10);
    }

    // Setup edge cards (Touch sensor loses sensitivity towards the edge).
    mCardAnchor[28].SetRightEdge(width);
  }

  @Override
  public void EventProcess(int event, CardAnchor anchor) {
    if (mIgnoreEvents) {
      return;
    }
    if (event == EVENT_DEAL) {
      if (mCardAnchor[28].GetCount() > 0) {
        mCardAnchor[29].AddCard(mCardAnchor[28].PopCard());
        if (mCardAnchor[28].GetCount() == 0) {
          mCardAnchor[28].SetDone(true);
        }
        mMoveHistory.push(new Move(28, 29, 1, true, false));
      }
    } else if (event == EVENT_STACK_TAP) {
      TryToSink(anchor);
    } else if (event == EVENT_STACK_ADD) {
      if ((mCardAnchor[28].GetCount() + mCardAnchor[29].GetCount()) == 52) {
        SignalWin();
      } else {
        MarkBlockedCards();
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
      for (int i = 0; i < 28; i++) {
        // Check for valid sink card candidate
        if (mCardAnchor[i].GetCount() > 0 && mCardAnchor[i].GetHiddenCount() <= 0) {
          Card card = mCardAnchor[i].PopCard();
          if (mCardAnchor[29].DropSingleCard(card)) {
            candidate = i;
            numCandidates++;
          }
          mCardAnchor[i].AddCard(card);
        }
      }
      // Sink card only if one and only one valid card candidate
      if (numCandidates == 1) {
        TryToSink(mCardAnchor[candidate]);
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
      if (mCardAnchor[29].DropSingleCard(card)) {
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
    if (mCardAnchor[29].DropSingleCard(card)) {
      mMoveHistory.push(new Move(anchor.GetNumber(), 29, 1, false, anchor.UnhideTopCard()));
      mAnimateCard.MoveCard(card, mCardAnchor[29]);
      return true;
    }

    return false;
  }

  @Override
  public String GetGameTypeString() {
    if (!mWrapCards) {
      return "TriPeaks";
    } else {
      return "TriPeaksWrapCards";
    }
  }

  @Override
  public String GetPrettyGameTypeString() {
    if (!mWrapCards) {
      return mView.GetContext().getResources().getString(R.string.menu_tripeaks);
    } else {
      return mView.GetContext().getResources().getString(R.string.menu_tripeaks_wrapcards);
    }
  }

}
