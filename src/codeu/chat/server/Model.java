// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package codeu.chat.server;

import java.util.Comparator;
import java.util.Map;

import java.util.HashMap;

import codeu.chat.common.Conversation;
import codeu.chat.common.ConversationSummary;
import codeu.chat.common.LinearUuidGenerator;
import codeu.chat.common.Message;
import codeu.chat.common.User;
import codeu.chat.util.Time;
import codeu.chat.util.Uuid;
import codeu.chat.util.store.Store;
import codeu.chat.util.store.StoreAccessor;
import codeu.chat.util.EncryptHelper;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.bson.Document;

public final class Model {
    
    //Connecting to the database
    MongoClient mongoClient = new MongoClient("localhost", 27017);
    MongoDatabase database = mongoClient.getDatabase("codeu");
    MongoCollection<Document> collection = database.getCollection("users");
    
    private static final Comparator<Uuid> UUID_COMPARE = new Comparator<Uuid>() {
        
    @Override
    public int compare(Uuid a, Uuid b) {

      if (a == b) { return 0; }

      if (a == null && b != null) { return -1; }

      if (a != null && b == null) { return 1; }

      final int order = Integer.compare(a.id(), b.id());
      return order == 0 ? compare(a.root(), b.root()) : order;
    }
  };

  private static final Comparator<Time> TIME_COMPARE = new Comparator<Time>() {
    @Override
    public int compare(Time a, Time b) {
      return a.compareTo(b);
    }
  };

  private static final Comparator<String> STRING_COMPARE = String.CASE_INSENSITIVE_ORDER;

  private final Store<Uuid, User> userById = new Store<>(UUID_COMPARE);
  private final Store<Time, User> userByTime = new Store<>(TIME_COMPARE);
  private final Store<String, User> userByText = new Store<>(STRING_COMPARE);
  private final Map<String, String> usernamePassword = new HashMap<>();
  private final Map<String, String> usernameSalt = new HashMap<>();

  private final Store<Uuid, Conversation> conversationById = new Store<>(UUID_COMPARE);
  private final Store<Time, Conversation> conversationByTime = new Store<>(TIME_COMPARE);
  private final Store<String, Conversation> conversationByText = new Store<>(STRING_COMPARE);

  private final Store<Uuid, Message> messageById = new Store<>(UUID_COMPARE);
  private final Store<Time, Message> messageByTime = new Store<>(TIME_COMPARE);
  private final Store<String, Message> messageByText = new Store<>(STRING_COMPARE);

  private final Uuid.Generator userGenerations = new LinearUuidGenerator(null, 1, Integer.MAX_VALUE);
  private Uuid currentUserGeneration = userGenerations.make();

  public void add(User user) {
    currentUserGeneration = userGenerations.make();

    userById.insert(user.id, user);
    userByTime.insert(user.creation, user);
    userByText.insert(user.name, user);
  }
  
  /**
   * Overloaded add method for users to add password support
   * @param user the User whose account to create
   * @param password the supplied password for this given account
   */
  public void add(User user, String password) {
    try {
        final String salt = EncryptHelper.getSalt();
        final String encPass = EncryptHelper.getSecurePassword(password, salt);
        usernameSalt.put(user.name, salt);
        usernamePassword.put(user.name, encPass);
        
        currentUserGeneration = userGenerations.make();

        userById.insert(user.id, user);
        userByTime.insert(user.creation, user);
        userByText.insert(user.name, user);
        
        //Also insert into database
        Document doc = new Document("username", user.name).append("id", user.id.toString()).append("time", user.creation.inMs()).append("password", encPass).append("salt", salt);
        collection.insertOne(doc);
    } catch (Exception e) {
        System.out.println("Error with encryption");
        e.printStackTrace();
    }
      
  }

  public StoreAccessor<Uuid, User> userById() {
    return userById;
  }

  public StoreAccessor<Time, User> userByTime() {
    return userByTime;
  }

  public StoreAccessor<String, User> userByText() {
    return userByText;
  }

  public Uuid userGeneration() {
    return currentUserGeneration;
  }

  public void add(Conversation conversation) {
    conversationById.insert(conversation.id, conversation);
    conversationByTime.insert(conversation.creation, conversation);
    conversationByText.insert(conversation.title, conversation);
  }

  public StoreAccessor<Uuid, Conversation> conversationById() {
    return conversationById;
  }

  public StoreAccessor<Time, Conversation> conversationByTime() {
    return conversationByTime;
  }

  public StoreAccessor<String, Conversation> conversationByText() {
    return conversationByText;
  }

  public void add(Message message) {
    messageById.insert(message.id, message);
    messageByTime.insert(message.creation, message);
    messageByText.insert(message.content, message);
  }

  public StoreAccessor<Uuid, Message> messageById() {
    return messageById;
  }

  public StoreAccessor<Time, Message> messageByTime() {
    return messageByTime;
  }

  public StoreAccessor<String, Message> messageByText() {
    return messageByText;
  }
  
  /**
   * Attempts to match the user attempt with the actual password associated with that username
   * @param name the username of the attempted login
   * @param attempt the password given by the user
   * @return whether the user supplied password matches the actual password for the given user
   */
  public boolean matchPassword(String name, String attempt) {
      if (!usernamePassword.containsKey(name) || !usernameSalt.containsKey(name)) return false;
      String encryptAttempt = EncryptHelper.getSecurePassword(attempt, usernameSalt.get(name));
      String correctPass = usernamePassword.get(name);
      
      return correctPass.equals(encryptAttempt);
  }
  
  /** 
   * Restores account information from the database.
   */
  public void syncModel() {
      MongoCursor<Document> cursor = collection.find().iterator();
      try {
          while (cursor.hasNext()) {
              final Document next = cursor.next();
              final String rawID = (String) next.get("id");
              final String realID = rawID.substring(rawID.indexOf(':')+1,rawID.indexOf(']')-1);
              final User user = new User(Uuid.parse(realID),(String) next.get("username"),Time.fromMs((long) next.get("time")));
              usernameSalt.put(user.name, (String) next.get("salt"));
              usernamePassword.put(user.name, (String) next.get("password"));
              
              userById.insert(user.id, user);
              userByTime.insert(user.creation, user);
              userByText.insert(user.name, user);
          }
      } catch (Exception e) {
          System.out.println("data error: unable to parse database info");
      
      } finally {
          cursor.close();
      }
  }
}