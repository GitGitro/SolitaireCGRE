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


class RulesFreecell extends Rules {

  private boolean mBySuit;
  private static int powerMoveMin = 0;

  public void Init(Bundle map) {
    mIgnoreEvents = true;
    mBySuit = mView.GetSettings().getBoolean("FreecellBuildBySuit", false);

    // Thirteen total anchors for regular solitaire
    mCardCount = 52;
    mCardAnchorCount = 16;
    mCardAnchor = new CardAnchor[mCardAnchorCount];

    // Top anchors for holding cards
    for (int i = 0; i < 4; i++) {
      mCardAnchor[i] = CardAnchor.CreateAnchor(CardAnchor.FREECELL_HOLD, i, this);
    }

    // Top anchors for sinking cards
    for (int i = 0; i < 4; i++) {
      mCardAnchor[i+4] = CardAnchor.CreateAnchor(CardAnchor.SEQ_SINK, i+4, this);
    }

    // Middle anchor stacks
    for (int i = 0; i < 8; i++) {
      if ( ! mBySuit ) {
        // Normal Freecell - build by alternate color
        mCardAnchor[i+8] = CardAnchor.CreateAnchor(CardAnchor.FREECELL_STACK, i+8,
                                                   this);
      } else {
        // Baker's Game - build by suit
        mCardAnchor[i+8] = CardAnchor.CreateAnchor(CardAnchor.GENERIC_ANCHOR, i+8, this);
        mCardAnchor[i+8].SetBuildSeq(GenericAnchor.SEQ_DSC);
        mCardAnchor[i+8].SetMoveSeq(GenericAnchor.SEQ_ASC);
        mCardAnchor[i+8].SetSuit(GenericAnchor.SUIT_SAME);
        mCardAnchor[i+8].SetWrap(false);
        mCardAnchor[i+8].SetPickup(GenericAnchor.PACK_LIMIT_BY_FREE);
        mCardAnchor[i+8].SetDropoff(GenericAnchor.PACK_LIMIT_BY_FREE);
        mCardAnchor[i+8].SetDisplay(GenericAnchor.DISPLAY_ALL);
      }
    }

    if (map != null) {
      // Do some assertions, default to a new game if we find an invalid state
      if (map.getInt("cardAnchorCount") == 16 &&
          map.getInt("cardCount") == 52) {
        int[] cardCount = map.getIntArray("anchorCardCount");
        int[] hiddenCount = map.getIntArray("anchorHiddenCount");
        int[] value = map.getIntArray("value");
        int[] suit = map.getIntArray("suit");
        int cardIdx = 0;

        for (int i = 0; i < 16; i++) {
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
    while (!mDeck.Empty()) {
      for (int i = 0; i < 8 && !mDeck.Empty(); i++) {
        mCardAnchor[i+8].AddCard(mDeck.PopCard());
      }
    }
    mIgnoreEvents = false;
  }

  public void Resize(int width, int height) {
    int rem = (width - (Card.WIDTH * 8)) / 8;
    for (int i = 0; i < 8; i++) {
      mCardAnchor[i].SetPosition(rem/2 + i * (rem + Card.WIDTH), 10);
      mCardAnchor[i+8].SetPosition(rem/2 + i * (rem + Card.WIDTH), 30 + Card.HEIGHT);
      mCardAnchor[i+8].SetMaxHeight(height - 30 - Card.HEIGHT);
    }

    // Setup edge cards (Touch sensor loses sensitivity towards the edge).
    mCardAnchor[0].SetLeftEdge(0);
    mCardAnchor[7].SetRightEdge(width);
    mCardAnchor[8].SetLeftEdge(0);
    mCardAnchor[15].SetRightEdge(width);
    for (int i = 0; i < 8; i++) {
      mCardAnchor[i+8].SetBottom(height);
    }
  }

  public void EventProcess(int event, CardAnchor anchor) {
    if (mIgnoreEvents) {
      return;
    }
    if (event == EVENT_STACK_ADD) {
      if (anchor.GetNumber() >= 4 && anchor.GetNumber() < 8) {
        if (mCardAnchor[4].GetCount() == 13 && mCardAnchor[5].GetCount() == 13 &&
            mCardAnchor[6].GetCount() == 13 && mCardAnchor[7].GetCount() == 13) {
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
  }

  @Override
  public boolean Fling(MoveCard moveCard) {
    if (moveCard.GetCount() == 1) {
      CardAnchor anchor = moveCard.GetAnchor();
      Card card = moveCard.DumpCards(false)[0];
      for (int i = 0; i < 4; i++) {
        if (mCardAnchor[i+4].DropSingleCard(card)) {
          EventAlert(EVENT_FLING, anchor, card);
          return true;
        }
      }
      anchor.AddCard(card);
    } else {
      moveCard.Release();
    }

    return false;
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

  private boolean TryToSink(CardAnchor anchor) {
    Card card = anchor.PopCard();
    boolean ret = TryToSinkCard(anchor, card);
    if (!ret) {
      anchor.AddCard(card);
    }
    return ret;
  }

  private boolean TryToSinkCard(CardAnchor anchor, Card card) {
    for (int i = 0; i < 4; i++) {
      if (mCardAnchor[i+4].DropSingleCard(card)) {
        mAnimateCard.MoveCard(card, mCardAnchor[i+4]);
        mMoveHistory.push(new Move(anchor.GetNumber(), i+4, 1, false, false));
        return true;
      }
    }

    return false;
  }

  @Override
  public void EventProcess(int event) {
    if (mIgnoreEvents == true) {
      return;
    }
    if (event == EVENT_SMART_MOVE) {
      for (int i = 0; i < 4; i++) {
        if (mCardAnchor[i].GetCount() > 0 &&
            TryToSink(mCardAnchor[i])) {
          return;
        }
      }
      for (int i = 0; i < 8; i++) {
        if (mCardAnchor[i+8].GetCount() > 0 &&
            TryToSink(mCardAnchor[i+8])) {
          return;
        }
      }
      mWasFling = false;
      mView.StopAnimating();
    }
  }

  private int CountFreeCells() {
    int freeCells = 0;
    for (int i = 0; i < 4; i++) {
      if (mCardAnchor[i].GetCount() == 0) {
        freeCells++;
      }
    }
    return freeCells;
  }

  private int CountFreeTableaus() {
    int freeTableaus = 0;
    for (int i = 0; i < 8; i++) {
      if (mCardAnchor[i+8].GetCount() == 0) {
        freeTableaus++;
      }
    }
    return freeTableaus;
  }

  private int CountPowerMoves(int freeCells, int freeTableaus) {
    // A freecell powermove is simply a shortcut move that lets you
    //   move a valid sequence of cards in a single move.  The same
    //   move can be made by moving cards one at at time to achieve
    //   the same result.
    //
    //   The number of cards that can be moved in a powermove in
    //   freecell is calculated as follows:
    //
    //   (1 + number of empty freecells) * 2 ^ (number of empty tableaus)
    //
    //   This assumes the sequence is moved to a non-empty tableau.
    //   If the move is into an empty tableau then this empty tableau
    //   does not count as an empty tableau for the formula above.

    // Calculate maximum sequence for powermove.
    //
    //   The maximum sequence occurs with a move onto a non-empty tableau.
    //
    int powerMoveMax = (1 + freeCells) * (int)Math.pow(2, freeTableaus);

    // Calculate minimum sequence for powermove at same time.
    //
    //   The minimum sequence occurs with a move into an empty
    //   tableau.
    //
    //   Calculating now is important due to the game popping the
    //   cards into a temporary move stack while dragging.  If the
    //   card anchor is emptied, then calculating the powermove
    //   immediately prior to dropping the move stack will erroneously
    //   include an extra empty tableau.
    //
    powerMoveMin = (1 + freeCells) * (int)Math.pow(2, Math.max(freeTableaus - 1, 0));

    return powerMoveMax;
  }

  @Override
  public int CountFreeSpaces() {
    // Assume the destination tableau is not an empty tableau.
    //   FreeSpaces = PowerMoves - 1
    return CountPowerMoves(CountFreeCells(), CountFreeTableaus()) - 1;
  }

  @Override
  public int CountFreeSpacesMin() {
    // Called when the destination tableau is an empty tableau
    //   and hence the tableau being moved into does not count
    //   as an empty tableau.
    //   FreeSpaces = PowerMoves - 1
    return powerMoveMin - 1;
  }

  @Override
  public String GetGameTypeString() {
    if ( ! mBySuit ) {
      return "FreecellBuildByAlternateColor";
    } else {
      return "FreecellBuildBySuit";
    }
  }

  @Override
  public String GetPrettyGameTypeString() {
    if ( ! mBySuit ) {
      return mView.GetContext().getResources().getString(R.string.menu_freecell);
    } else {
      return mView.GetContext().getResources().getString(R.string.menu_bakersgame);
    }
  }
}
