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

  Modified by Curtis Gedak 2015
*/
package net.sourceforge.solitaire_cg_re;

import java.util.Random;


public class Deck {

  private Card[] mCard;
  private int mCardCount;
  private CustomRandom mRand;

  public Deck(int decks) {
    Init(decks, 4);
  }

  public Deck(int decks, int suits) {
    if (suits == 2) {
      decks *= 2;
    } else if (suits == 1) {
      decks *= 4;
    }
    Init(decks, suits);
  }

  private void Init(int decks, int suits) {
    mCardCount = decks * 13 * suits;
    mCard = new Card[mCardCount];
    for (int deck = 0; deck < decks; deck++) {
      for (int suit = Card.CLUBS; suit < suits; suit++) {
        for (int value = 0; value < 13; value++) {
          mCard[deck*suits*13 + suit*Card.KING + value] = new Card(value+1, suit);
        }
      }
    }

    mRand = new CustomRandom();

    Shuffle();
    Shuffle();
    Shuffle();
  }

  public void PushCard(Card card) {
    mCard[mCardCount++] = card;
  }

  public Card PopCard() {
    if (mCardCount > 0) {
      return mCard[--mCardCount];
    }
    return null;
  }

  public boolean Empty() {
    return mCardCount == 0;
  }

  public void Shuffle() {
    int lastIdx = mCardCount - 1;
    int swapIdx;
    Card swapCard;

    while (lastIdx > 1) {
      swapIdx = mRand.nextInt(lastIdx);
      swapCard = mCard[swapIdx];
      mCard[swapIdx] = mCard[lastIdx];
      mCard[lastIdx] = swapCard;
      lastIdx--;
    }
  }
}

//PCG-32-XSH-RR Algorithm
//See http://pcg-random.org
//Also https://github.com/imneme/pcg-c-basic/blob/master/pcg_basic.c
class CustomRandom {

  private long mState;

  CustomRandom(){
    mState = System.nanoTime() ^ 181783497276652981L;
  }

  //pcg32_random_r()
  int next() {
    long oldstate = mState;
    mState = oldstate * 6364136223846793005L + 1;
    int xorshifted = (int) (((oldstate >>> 18) ^ oldstate) >>> 27);
    int rot = (int)(oldstate >>> 59);
    return (xorshifted >>> rot) | (xorshifted << ((-rot) & 31));
  }
  //pcg32_boundedrand_r()
  int nextInt(final int bound){
    //lack of unsigned division in Java prevents us from optimizing like the original
    //so we do the modulo operations in 64-bit :(
    final long lbound = (long) bound;
    final long threshold = 0x100000000L % lbound;
    for (;;) {
      long r = (long)this.next() & 0x00000000FFFFFFFFL;
      if (r >= threshold)
        return (int) (r % bound);
    }
  }
}