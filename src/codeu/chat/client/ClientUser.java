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

package codeu.chat.client;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import codeu.chat.common.User;
import codeu.chat.util.Logger;
import codeu.chat.util.Uuid;
import codeu.chat.util.store.Store;

public final class ClientUser {

  private final static Logger.Log LOG = Logger.newLog(ClientUser.class);

  private static final Collection<Uuid> EMPTY = Arrays.asList(new Uuid[0]);
  private final Controller controller;
  private final View view;

  private User current = null;

  private final Map<Uuid, User> usersById = new HashMap<>();

  // This is the set of users known to the server, sorted by name.
  private Store<String, User> usersByName = new Store<>(String.CASE_INSENSITIVE_ORDER);
  private final Map<String, User> usersNames = new HashMap<>();


  public ClientUser(Controller controller, View view) {
    this.controller = controller;
    this.view = view;
  }

  // Validate the username string
  public boolean isValidName(String userName) {
    boolean clean = true;
    if (userName.length() == 0) {
      clean = false;
    } else {

      if (usersNames.containsKey(userName)) {
        clean = false;
      }

    }
    return clean;
  }

  public boolean hasCurrent() {
    return (current != null);
  }

  public User getCurrent() {
    return current;
  }

  public boolean signInUser(String username, String password) {
    updateUsers();

    final User prev = current;
    
    if (view.matchPassword(username, password)) {

      final User newCurrent = usersByName.first(username);

      if (newCurrent != null) {
        current = newCurrent;
      }
      System.out.println("Sign in as " + current.name + " successful");
    }
    return (prev != current);
  }

  public boolean signOutUser() {
    boolean hadCurrent = hasCurrent();
    current = null;
    return hadCurrent;
  }

  public void showCurrent() {
    printUser(current);
  }

  public void addUser(String name, String password) {
    final boolean validInputs = isValidName(name);
    final boolean exists = usersNames.containsKey(name);

    final User user = (validInputs) ? controller.newUser(name, password) : null;

    if (user == null) {
      System.out.format("Error: user not created - %s.\n",
          (!exists) ? "name failure" : "username already exists",
          (validInputs) ? "server failure" : "bad input value");
    } else {
      LOG.info("New user complete, Name= \"%s\" UUID=%s", user.name, user.id);
      updateUsers();
    }
  }

  public void showAllUsers() {
    updateUsers();
    for (final User u : usersByName.all()) {
      printUser(u);
    }
  }
  
  /**
   * Returns an immutable set of all usernames known to the system.
   * Used to allow signing in to fail fast if requested user does not exist
   */
  public Set<String> getAllUsernames() {
      updateUsers();
      return Collections.unmodifiableSet(usersNames.keySet());
  }

  public User lookup(Uuid id) {
    return (usersById.containsKey(id)) ? usersById.get(id) : null;
  }

  public String getName(Uuid id) {
    final User user = lookup(id);
    if (user == null) {
      LOG.warning("userContext.lookup() failed on ID: %s", id);
      return null;
    } else {
      return user.name;
    }
  }

  public Iterable<User> getUsers() {
    return usersByName.all();
  }

  public void updateUsers() {
    usersById.clear();
    usersByName = new Store<>(String.CASE_INSENSITIVE_ORDER);

    for (final User user : view.getUsersExcluding(EMPTY)) {
      usersById.put(user.id, user);
      usersByName.insert(user.name, user);
      usersNames.put(user.name, user);
    }
  }

  public static String getUserInfoString(User user) {
    return (user == null) ? "Null user" :
        String.format(" User: %s\n   Id: %s\n   created: %s\n", user.name, user.id, user.creation);
  }

  public String showUserInfo(String uname) {
    return getUserInfoString(usersByName.first(uname));
  }

  public static void printUser(User user) {
    System.out.println(getUserInfoString(user));
  }

}
