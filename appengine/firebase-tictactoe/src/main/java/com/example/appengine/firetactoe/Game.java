/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.appengine.firetactoe;

import com.google.common.io.CharStreams;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.RuntimeException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Entity
public class Game {
  static final Pattern[] XWins =
      {Pattern.compile("XXX......"), Pattern.compile("...XXX..."), Pattern.compile("......XXX"),
          Pattern.compile("X..X..X.."), Pattern.compile(".X..X..X."),
          Pattern.compile("..X..X..X"), Pattern.compile("X...X...X"),
          Pattern.compile("..X.X.X..")};
  static final Pattern[] OWins =
      {Pattern.compile("OOO......"), Pattern.compile("...OOO..."), Pattern.compile("......OOO"),
          Pattern.compile("O..O..O.."), Pattern.compile(".O..O..O."),
          Pattern.compile("..O..O..O"), Pattern.compile("O...O...O"),
          Pattern.compile("..O.O.O..")};

  @Id
  public String id;
  public String userX;
  public String userO;
  public String board;
  public Boolean moveX;
  public String winner;
  public String winningBoard;

  private static final String SERVICE_ACCOUNT_PATH = "resources/credentials.json";
  private static final String FIREBASE_SNIPPET_PATH = "resources/firebase_config.html";
  private static String sFirebaseDbUrl;
  protected static String sFirebaseSnippet;

  static {
    try {
      sFirebaseSnippet = CharStreams.toString(
          new InputStreamReader(new FileInputStream(FIREBASE_SNIPPET_PATH)));
      sFirebaseDbUrl = parseFirebaseUrl(sFirebaseSnippet);

      FirebaseOptions options = new FirebaseOptions.Builder()
          .setServiceAccount(new FileInputStream(SERVICE_ACCOUNT_PATH))
          .setDatabaseUrl(sFirebaseDbUrl)
          .build();
      FirebaseApp.initializeApp(options);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static String parseFirebaseUrl(String firebaseSnippet) {
    int idx = firebaseSnippet.indexOf("databaseURL");
    idx = firebaseSnippet.indexOf(':', idx + 11);
    int openQuote = firebaseSnippet.indexOf('"', idx);
    int closeQuote = firebaseSnippet.indexOf('"', openQuote + 1);
    return firebaseSnippet.substring(openQuote + 1, closeQuote);
  }

  Game() {
  }

  Game(String userX, String userO, String board, boolean moveX) {
    this.id = UUID.randomUUID().toString();
    this.userX = userX;
    this.userO = userO;
    this.board = board;
    this.moveX = moveX;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUserX() {
    return userX;
  }

  public String getUserO() {
    return userO;
  }

  public void setUserO(String userO) {
    this.userO = userO;
  }

  public String getBoard() {
    return board;
  }

  public void setBoard(String board) {
    this.board = board;
  }

  public boolean getMoveX() {
    return moveX;
  }

  public void setMoveX(boolean moveX) {
    this.moveX = moveX;
  }

  public String getMessageString() {
    Map<String, String> state = new HashMap<String, String>();
    state.put("userX", userX);
    if (userO == null) {
      state.put("userO", "");
    } else {
      state.put("userO", userO);
    }
    state.put("board", board);
    state.put("moveX", moveX.toString());
    state.put("winner", winner);
    if (winner != null && winner != "") {
      state.put("winningBoard", winningBoard);
    }
    JSONObject message = new JSONObject(state);
    return message.toString();
  }

  //[START send_updates]
  public String getChannelKey(String user) {
    return user + id;
  }

  public void deleteChannel(String user) {
    if (user != null) {
      String channelKey = getChannelKey(user);
      final FirebaseDatabase database = FirebaseDatabase.getInstance();
      DatabaseReference ref = database.getReference("channels");
      ref.child(channelKey).setValue(null);
    }
  }

  private void sendUpdateToUser(String user) {
    if (user != null) {
      String channelKey = getChannelKey(user);
      final FirebaseDatabase database = FirebaseDatabase.getInstance();
      DatabaseReference ref = database.getReference("channels");
      ref.child(channelKey).setValue(this);
    }
  }

  public void sendUpdateToClients() {
    sendUpdateToUser(userX);
    sendUpdateToUser(userO);
  }
  //[END send_updates]

  public void checkWin() {
    final Pattern[] wins;
    if (moveX) {
      wins = XWins;
    } else {
      wins = OWins;
    }

    for (Pattern winPattern : wins) {
      if (winPattern.matcher(board).matches()) {
        if (moveX) {
          winner = userX;
        } else {
          winner = userO;
        }
        winningBoard = winPattern.toString();
      }
    }
  }

  //[START make_move]
  public boolean makeMove(int position, String user) {
    String currentMovePlayer;
    char value;
    if (getMoveX()) {
      value = 'X';
      currentMovePlayer = getUserX();
    } else {
      value = 'O';
      currentMovePlayer = getUserO();
    }

    if (currentMovePlayer.equals(user)) {
      char[] boardBytes = getBoard().toCharArray();
      boardBytes[position] = value;
      setBoard(new String(boardBytes));
      checkWin();
      setMoveX(!getMoveX());
      sendUpdateToClients();
      return true;
    }

    return false;
  }
  //[END make_move]
}
