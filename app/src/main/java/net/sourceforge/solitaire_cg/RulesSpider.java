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

  Modified by Curtis Gedak 2015, 2016, 2017
*/
package net.sourceforge.solitaire_cg;

import android.os.Bundle;


class RulesSpider extends Rules {
  private int mStillDealingStack;
  public void Init(Bundle map) {
    mIgnoreEvents = true;
    mStillDealingStack = 10; // Value > last anchor stack (9)

    mCardCount = 104;
    mCardAnchorCount = 12;
    mCardAnchor = new CardAnchor[mCardAnchorCount];

    // Anchor stacks
    for (int i = 0; i < 10; i++) {
      mCardAnchor[i] = CardAnchor.CreateAnchor(CardAnchor.GENERIC_ANCHOR, i, this);
      mCardAnchor[i].SetBuildSeq(GenericAnchor.SEQ_DSC);
      mCardAnchor[i].SetBuildSuit(GenericAnchor.SEQ_ANY);
      mCardAnchor[i].SetMoveSeq(GenericAnchor.SEQ_ASC);
      mCardAnchor[i].SetMoveSuit(GenericAnchor.SUIT_SAME);
      mCardAnchor[i].SetBehavior(GenericAnchor.PACK_MULTI);
      mCardAnchor[i].SetDisplay(GenericAnchor.DISPLAY_MIX);
      mCardAnchor[i].SetHack(GenericAnchor.DEALHACK);
    }

    mCardAnchor[10] = CardAnchor.CreateAnchor(CardAnchor.DEAL_FROM, 10, this);
    mCardAnchor[11] = CardAnchor.CreateAnchor(CardAnchor.DEAL_TO, 11, this);

    if (map != null) {
      // Do some assertions, default to a new game if we find an invalid state
      if (map.getInt("cardAnchorCount") == 12 &&
          map.getInt("cardCount") == 104) {
        int[] cardCount = map.getIntArray("anchorCardCount");
        int[] hiddenCount = map.getIntArray("anchorHiddenCount");
        int[] value = map.getIntArray("value");
        int[] suit = map.getIntArray("suit");
        int cardIdx = 0;

        for (int i = 0; i < mCardAnchorCount; i++) {
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

    int suits = mView.GetSettings().getInt("SpiderSuits", 4);
    mDeck = new Deck(2, suits);
    int i = 54;
    while (i > 0) {
      for (int j = 0; j < 10 && i > 0; j++) {
        i--;
        mCardAnchor[j].AddCard(mDeck.PopCard());
        mCardAnchor[j].SetHiddenCount(mCardAnchor[j].GetCount() - 1);
      }
    }

    while (!mDeck.Empty()) {
      mCardAnchor[10].AddCard(mDeck.PopCard());
    }
    mIgnoreEvents = false;
  }

  public void Resize(int width, int height) {
    int rem = (width - (Card.WIDTH * 10)) / 10;
    for (int i = 0; i < 10; i++) {
      mCardAnchor[i].SetPosition(rem/2 + i * (rem + Card.WIDTH), 10);
      if (i != 0) {
        mCardAnchor[i].SetMaxHeight(height-10);
      } else {
        // Leave room for deck at bottom left of screen
        mCardAnchor[i].SetMaxHeight(height - Card.HEIGHT - 2*10);
      }
    }
    // Setup edge cards (Touch sensor loses sensitivity towards the edge).
    mCardAnchor[0].SetLeftEdge(0);
    mCardAnchor[9].SetRightEdge(width);

    for (int i = 0; i < 10; i++) {
      mCardAnchor[i].SetBottom(height);
    }
    // Setup deck in bottom left corner
    mCardAnchor[10].SetPosition(-Card.WIDTH * 2, 1);
    mCardAnchor[10].SetPosition(rem/2, height - Card.HEIGHT - 10);
    mCardAnchor[10].SetMaxHeight(height-10);
    // This is offscreen as the user doesn't need to see it, but it is
    // needed to hold onto out of play cards.
    mCardAnchor[11].SetPosition(-Card.WIDTH * 2, 1);
  }

  @Override
  public void EventProcess(int event) {
  }

  @Override
  public void EventProcess(int event, CardAnchor anchor, Card card) {
    anchor.AddCard(card);
  }

  @Override
  public void EventProcess(int event, CardAnchor anchor) {
    if (mIgnoreEvents) {
      return;
    }
    if (event == EVENT_STACK_ADD) {
      if (anchor.GetCount() - anchor.GetHiddenCount() >= 13) {
        Card[] card = anchor.GetCards();
        if (card[anchor.GetCount()-1].GetValue() == 1) {
          int suit = card[anchor.GetCount()-1].GetSuit();
          int val = 2;
          for (int i = anchor.GetCount() - 2; i >= 0 && val < 14; i--, val++) {
            if (card[i].GetValue() != val || card[i].GetSuit() != suit) {
              break;
            }
          }
          if (val == 14) {
            for (int j = 0; j < 13; j++) {
              mCardAnchor[11].AddCard(anchor.PopCard());
            }
            mMoveHistory.push(new Move(anchor.GetNumber(), 11, 13, true, anchor.UnhideTopCard()));

            if (mCardAnchor[11].GetCount() == mCardCount) {
              SignalWin();
            }
          }
        }
      }
      if (mStillDealingStack < 10) {
        // Post another event if we aren't done yet.
        mStillDealingStack = anchor.GetNumber()+1;
        EventAlert(EVENT_DEAL_NEXT, mCardAnchor[mStillDealingStack]);
      }
    } else if (event == EVENT_DEAL) {
      if (mCardAnchor[10].GetCount() > 0) {
        int count = mCardAnchor[10].GetCount() > 10 ? 10 : mCardAnchor[10].GetCount();
        mAnimateCard.MoveCard(mCardAnchor[10].PopCard(), mCardAnchor[0]);
        mMoveHistory.push(new Move(10, 0, count-1, 1, false, false));
        mStillDealingStack = 0; // First anchor stack
      }
    } else if (event == EVENT_DEAL_NEXT) {
      if (mCardAnchor[10].GetCount() > 0 && anchor.GetNumber() < 10) {
        mAnimateCard.MoveCard(mCardAnchor[10].PopCard(), anchor);
        mStillDealingStack = anchor.GetNumber();
      } else {
        mView.StopAnimating();
        mStillDealingStack = 10;
      }
    }
  }

  @Override
  public void FinishDeal() {
    // Invoked if game interrupted, for example by a phone call
    if (mStillDealingStack < 10) {
      // Ensure multi-stack deal is finished
      while (mCardAnchor[10].GetCount() > 0 && mStillDealingStack < 10) {
	mCardAnchor[++mStillDealingStack].AddCard(mCardAnchor[10].PopCard());
      }
      mStillDealingStack = 10;
    }
  }

  @Override
  public String GetGameTypeString() {
    int suits = mView.GetSettings().getInt("SpiderSuits", 4);
    if (suits == 1) {
      return "Spider1Suit";
    } else if (suits == 2) {
      return "Spider2Suit";
    } else {
      return "Spider4Suit";
    }
  }

  @Override
  public String GetPrettyGameTypeString() {
    int suits = mView.GetSettings().getInt("SpiderSuits", 4);
    if (suits == 1) {
      return mView.GetContext().getResources().getString(R.string.menu_blackwidow);
    } else if (suits == 2) {
      return mView.GetContext().getResources().getString(R.string.menu_tarantula);
    } else {
      return mView.GetContext().getResources().getString(R.string.menu_spider);
    }
  }

}
