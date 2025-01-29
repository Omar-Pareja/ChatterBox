import java.util.*;

/*
 * Make sure to write your own tests in ServerModelTest.java.
 * The tests we provide for each task are NOT comprehensive!
 */

/**
 * The {@code ServerModel} is the class responsible for tracking the
 * state of the server, including its current users and the channels
 * they are in.
 * This class is used by subclasses of {@link Command} to:
 * 1. handle commands from clients, to create channels, send messages, etc.
 * and
 * 2. handle commands from {@link ServerBackend} to coordinate
 * client connection/disconnection.
 */
public final class ServerModel {
    private final Map<Integer, String> userMap;
    private final Map<String, Set<Integer>> channelMap;
    private final Map<String, Integer> channelOwners;

    /**
     * Constructs a {@code ServerModel}. Make sure to initialize any collections
     * used to model the server state here.
     */
    public ServerModel() {
        userMap = new TreeMap<>();
        channelMap = new TreeMap<>();
        channelOwners = new TreeMap<>();
    }

    // =========================================================================
    // == Task 2: Basic Server model queries
    // == These functions provide helpful ways to test the state of your model.
    // == You may also use them in later tasks to process commands.
    // =========================================================================

    /**
     * Is this an existing nickname?
     *
     * @param nickname any string
     * @return whether the nickname is currently in use by any user in any channel
     */
    public boolean existingNickname(String nickname) {
        return userMap.containsValue(nickname);
    }

    /**
     * Is this a userId that is known to the server?
     *
     * @param userId any integer
     * @return whether the userId has been registered for any client
     */
    public boolean existingUserId(Integer userId) {
        return userMap.containsKey(userId);
    }

    /**
     * Gets the userId currently associated with the given nickname.
     *
     * @param userNickname The nickname for which to get the associated userId
     * @return The userId of the user with the provided nickname if
     *         such a user exists
     * @throws IllegalArgumentException if there is no userId associated
     *                                  with the nickname.
     */
    public int getUserId(String userNickname) {
        for (Map.Entry<Integer, String> entry : userMap.entrySet()) {
            if (entry.getValue().equals(userNickname)) {
                return entry.getKey();
            }
        }
        throw new IllegalArgumentException("Unknown nickname: " + userNickname);
    }

    /**
     * Gets the nickname currently associated with the given userId.
     *
     * @param userId The userId for which to get the associated
     *               nickname
     * @return The nickname of the user with that userId if
     *         such a user exists
     * @throws IllegalArgumentException if the userId is not in use
     */
    public String getNickname(int userId) {
        if (userMap.containsKey(userId)) {
            return userMap.get(userId);
        }
        throw new IllegalArgumentException("User Id " + userId + " has no associate nickname.");
    }

    /**
     * Gets a collection of the nicknames of all users who are
     * registered with the server. Modifications to the returned collection
     * should not affect the server state.
     *
     * This method is provided for testing.
     *
     * @return The collection of registered user nicknames
     */
    public Collection<String> getRegisteredUsers() {
        return new ArrayList<>(userMap.values());
    }

    /**
     * Gets a collection of the names of all the channels that are
     * present on the server. Modifications to the returned collection
     * should not affect the server state.
     *
     * This method is public for testing.
     *
     * @return The collection of channel names
     */
    public Collection<String> getChannels() {
        return new ArrayList<>(channelMap.keySet());
    }

    /**
     * Gets a collection of the userIds of all users who are
     * in the channel. Modifications to the returned collection
     * should not affect the server state.
     *
     * This method is public for testing.
     *
     * @param channelName The channel
     * @return The collection of IDs
     * @throws IllegalArgumentException if the channel does not exist
     */
    public Collection<Integer> getUserIdsInChannel(String channelName) {
        if (!channelMap.containsKey(channelName)) {
            throw new IllegalArgumentException("Unknown channel: " + channelName);
        }
        return new HashSet<>(channelMap.get(channelName));
    }

    /**
     * Gets an alphabetically sorted set of the nicknames of all the users
     * in a given channel. Modifications to the returned collection should
     * not affect the server state.
     *
     * This method is public for testing.
     *
     * @param channelName The channel for which to get member nicknames
     * @return A collection of all user nicknames in the channel
     * @throws IllegalArgumentException if there is no channel with the given name
     */
    public SortedSet<String> getUserNicknamesInChannel(String channelName) {
        if (!channelMap.containsKey(channelName)) {
            throw new IllegalArgumentException("Unknown channel: " + channelName);
        }
        SortedSet<String> nicknames = new TreeSet<>();
        for (Integer userId : channelMap.get(channelName)) {
            nicknames.add(getNickname(userId));
        }
        return nicknames;
    }

    /**
     * Gets the nickname of the owner of the given channel.
     *
     * This method is provided for testing.
     *
     * @param channelName The channel for which to get the owner nickname
     * @return The nickname of the channel owner if such a channel exists
     * @throws IllegalArgumentException if there is no channel with
     *                                  the given name
     */
    public String getOwner(String channelName) {
        if (!channelOwners.containsKey(channelName)) {
            throw new IllegalArgumentException("Unknown channel: " + channelName);
        }
        int ownerId = channelOwners.get(channelName);
        return getNickname(ownerId);
    }


    // ===============================================
    // == Task 3: Connections and Setting Nicknames ==
    // ===============================================

    /**
     * This method is automatically called when a new client connects
     * to the server. It should generate a default nickname with
     * {@link #generateUniqueNickname()}, store the new userId and nickname
     * in your {@link ServerModel} state.
     * The method should return a single {@link Response#connected} response to the
     * client.
     *
     * @param userId The new user's unique identifier (created by the backend)
     * @return A {@link ResponseSet} object indicating that the connection was
     *         successful.
     */
    public ResponseSet registerUser(int userId) {
        String userNickname = generateUniqueNickname();
        userMap.put(userId, userNickname);
        return ResponseSet.singleMessage(Response.connected(userId, userNickname));
    }

    /**
     * Helper for {@link #registerUser(int)}. (Nothing to do here.)
     *
     * Generates a unique nickname of the form "UserX", where X is the
     * smallest non-negative integer that yields a unique nickname for a user.
     *
     * @return The generated nickname
     */
    private String generateUniqueNickname() {
        int suffix = 0;
        String userNickname;
        Collection<String> existingUsers = getRegisteredUsers();
        do {
            userNickname = "User" + suffix++;
        } while (existingUsers.contains(userNickname));
        return userNickname;
    }

    /**
     * This method is automatically called when a client
     * disconnects from the server. This method should take the following
     * actions, not necessarily in this order, and notify other users
     * that the client has left.
     *
     * (1) The disconnected user's information should be deleted from
     * the {@link ServerModel}'s internal state
     * (2) All channels owned by the disconnected user should be deleted
     * the {@link ServerModel}'s internal state
     *
     * @param userId The unique userId of the user to deregister
     * @return a {@link ResponseSet} object containing {@link Response#disconnected}
     *         responses addressed to all users who shared a channel with
     *         the disconnected user, excluding the disconnected user.
     * @throws IllegalArgumentException if the userID is not currently registered.
     *                                  (If
     *                                  you correctly use previous methods you've
     *                                  written, this could happen
     *                                  automatically!)
     *
     */
    public ResponseSet deregisterUser(int userId) {
        if (!userMap.containsKey(userId)) {
            throw new IllegalArgumentException("User ID not registered: " + userId);
        }
        String userNickname = userMap.get(userId);
        ResponseSet responses = new ResponseSet();

        for (String channelName : new HashSet<>(channelMap.keySet())) {
            Set<Integer> members = channelMap.get(channelName);

            if (members.contains(userId)) {
                members.remove(userId);

                for (int memberId : members) {
                    responses.addMessage(Response.disconnected(memberId, userNickname));
                }

                if (channelOwners.get(channelName) == userId || members.isEmpty()) {
                    channelOwners.remove(channelName);
                    channelMap.remove(channelName);
                }
            }
        }

        userMap.remove(userId);
        return responses;
    }

    /**
     * This method is called when a user wants to change their nickname.
     *
     * @param nickCommand The {@link NicknameCommand} object containing
     *                    all information needed to attempt a nickname change
     * @return If the nickname change is successful,
     *         a {@link ResponseSet} containing a {@link Response#okay} response for
     *         each user that shares at least one channel with the sender, including
     *         the sender. Each response should contain the original
     *         {@link NicknameCommand},
     *         and the old nickname.
     *
     *         If an error occurs, use
     *         {@link Response#error} with either:
     *         (1) {@link ErrorCode#INVALID_NAME} if the proposed nickname
     *         is not valid according to
     *         {@link ServerModel#isValidName(String)}
     *         (2) {@link ErrorCode#NAME_ALREADY_IN_USE} if there is
     *         already a user with the proposed nickname. This includes
     *         the current nickname of the user requesting the change.
     */
    public ResponseSet changeNickname(NicknameCommand nickCommand) {
        int userId = nickCommand.getSenderId();
        String newNickname = nickCommand.getNewNickname();

        if (!isValidName(newNickname)) {
            return ResponseSet.singleMessage(Response.error(nickCommand, ErrorCode.INVALID_NAME));
        }

        if (existingNickname(newNickname)) {
            return ResponseSet.singleMessage(Response.error(nickCommand,
                    ErrorCode.NAME_ALREADY_IN_USE));
        }

        String oldNickname = userMap.get(userId);
        userMap.put(userId, newNickname);

        ResponseSet responseSet = new ResponseSet();
        responseSet.addMessage(Response.okay(userId, oldNickname, nickCommand));
        for (String channelName : channelMap.keySet()) {
            if (channelMap.get(channelName).contains(userId)) {
                for (Integer memberId : channelMap.get(channelName)) {
                    if (!memberId.equals(userId)) {
                        responseSet.addMessage(Response.okay(memberId, oldNickname, nickCommand));
                    }
                }
            }
        }
        return responseSet;
    }

    /**
     * Determines if a given name is valid or invalid (contains at least
     * one alphanumeric character, and no non-alphanumeric characters).
     * (Nothing to do here.)
     *
     * @param name The channel or nickname string to validate
     * @return true if the string is a valid name
     */
    public static boolean isValidName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        for (char c : name.toCharArray()) {
            if (!Character.isLetterOrDigit(c)) {
                return false;
            }
        }
        return true;
    }
    // ===================================
    // == Task 4: Channels and Messages ==
    // ===================================

    /**
     * This method is called when a user wants to create a channel.
     * You can ignore the privacy aspect of this method for task 4, but
     * if you choose to do the task 5 Kudos problem, make sure you come back
     * and implement the privacy aspect.
     *
     * @param createCommand The {@link CreateCommand} object containing all
     *                      information needed to attempt channel creation
     * @return If the channel creation is successful, {@link ResponseSet} containing
     *         a
     *         single {@link Response#okay} message. The recipient should be owner
     *         of
     *         the new channel.
     *
     *         If an error occurs, use
     *         {@link Response#error} with either:
     *         (1) {@link ErrorCode#INVALID_NAME} if the proposed
     *         channel name is not valid according to
     *         {@link ServerModel#isValidName(String)}
     *         (2) {@link ErrorCode#CHANNEL_ALREADY_EXISTS} if there is
     *         already a channel with the proposed name
     */

    public ResponseSet createChannel(CreateCommand createCommand) {
        String channelName = createCommand.getChannel();
        int ownerId = createCommand.getSenderId();

        if (!isValidName(channelName)) {
            return ResponseSet.singleMessage(Response.error(
                    createCommand, ErrorCode.INVALID_NAME));
        }

        if (channelMap.containsKey(channelName)) {
            return ResponseSet.singleMessage(Response.error(
                    createCommand, ErrorCode.CHANNEL_ALREADY_EXISTS));
        }

        channelMap.put(channelName, new TreeSet<>(Collections.singleton(ownerId)));
        channelOwners.put(channelName, ownerId);

        return ResponseSet.singleMessage(Response.okay(
                ownerId, getNickname(ownerId), createCommand));
    }



    /**
     * This method is called when a user wants to join a channel.
     *
     * You can ignore the privacy aspect of this method for task 4.
     * If you choose to do the task 5 Kudos problem, make sure you come
     * back and implement the privacy aspect.
     *
     * @param joinCommand The {@link JoinCommand} object containing all
     *                    information needed for the user's join attempt
     * @return If the user can successfully join the channel, the
     *         {@link ResponseSet} should include a {@link Response#okay} response
     *         sent to all users in the channel (including the newly joined user).
     *         Furthermore, the {@link ResponseSet} should include a
     *         {@link Response#names}
     *         response to the newly joined user containing the names of all users
     *         in the channel (also including the newly joined user).
     *
     *         If an error occurs, use
     *         {@link Response#error} with either:
     *         (1) {@link ErrorCode#NO_SUCH_CHANNEL} if there is no
     *         channel with the specified name
     *         (2) (after Task 5) {@link ErrorCode#JOIN_PRIVATE_CHANNEL} if
     *         the sender is attempting to join a private channel
     */
    public ResponseSet joinChannel(JoinCommand joinCommand) {
        int userId = joinCommand.getSenderId();
        String channelName = joinCommand.getChannel();

        if (!channelMap.containsKey(channelName)) {
            return ResponseSet.singleMessage(Response.error(
                    joinCommand, ErrorCode.NO_SUCH_CHANNEL));
        }

        channelMap.get(channelName).add(userId);

        ResponseSet responses = new ResponseSet();

        for (int memberId : channelMap.get(channelName)) {
            responses.addMessage(Response.okay(memberId, getNickname(userId), joinCommand));
        }

        SortedSet<String> userNicknames = new TreeSet<>();
        for (int memberId : channelMap.get(channelName)) {
            userNicknames.add(getNickname(memberId));
        }

        String channelOwner = getNickname(channelOwners.get(channelName));
        responses.addMessage(Response.names(
                userId, getNickname(userId), channelName, userNicknames, channelOwner));
        return responses;
    }

    /**
     * This method is called when a user wants to send a message to a channel.
     *
     * @param messageCommand The {@link MessageCommand} object containing all
     *                       information needed for the messaging attempt.
     * @return If the message can be sent, the {@link ResponseSet} should include
     *         {@link Response#okay} responses addressed to all clients in the
     *         channel.
     *
     *         If an error occurs, use
     *         {@link Response#error} with either:
     *         (1) {@link ErrorCode#NO_SUCH_CHANNEL} if there is no
     *         channel with the specified name
     *         (2) {@link ErrorCode#USER_NOT_IN_CHANNEL} if the sender is
     *         not in the channel they are trying to send the message to
     */
    public ResponseSet sendMessage(MessageCommand messageCommand) {
        int senderId = messageCommand.getSenderId();
        String channelName = messageCommand.getChannel();

        if (!channelMap.containsKey(channelName)) {
            return ResponseSet.singleMessage(Response.error(
                    messageCommand, ErrorCode.NO_SUCH_CHANNEL));
        }

        if (!channelMap.get(channelName).contains(senderId)) {
            return ResponseSet.singleMessage(Response.error(
                    messageCommand, ErrorCode.USER_NOT_IN_CHANNEL));
        }

        ResponseSet responses = new ResponseSet();

        for (int memberId : channelMap.get(channelName)) {
            responses.addMessage(Response.okay(memberId, getNickname(senderId), messageCommand));
        }

        return responses;
    }

    /**
     * This method is called when a user wants to leave a channel.
     *
     * @param leaveCommand The {@link LeaveCommand} object containing all
     *                     information about the user's leave attempt
     * @return A {@link ResponseSet} object containing {@link Response#okay}
     *         responses to all users in the channel (including the leaving
     *         user), informing them that the leave command was successful.
     *         If the user was the last member of the channel, then the channel
     *         is also removed.
     *
     *         If an error occurs, use
     *         {@link Response#error} with either:
     *         (1) {@link ErrorCode#NO_SUCH_CHANNEL} if there is no
     *         channel with the specified name
     *         (2) {@link ErrorCode#USER_NOT_IN_CHANNEL} if the sender is
     *         not in the channel they are trying to leave
     */
    public ResponseSet leaveChannel(LeaveCommand leaveCommand) {
        int senderId = leaveCommand.getSenderId();
        String channelName = leaveCommand.getChannel();

        if (!channelMap.containsKey(channelName)) {
            return ResponseSet.singleMessage(Response.error(
                    leaveCommand, ErrorCode.NO_SUCH_CHANNEL));
        }

        if (!channelMap.get(channelName).contains(senderId)) {
            return ResponseSet.singleMessage(Response.error(
                    leaveCommand, ErrorCode.USER_NOT_IN_CHANNEL));
        }
        ResponseSet responses = new ResponseSet();
        channelMap.get(channelName).remove(senderId);
        for (int memberId : channelMap.get(channelName)) {
            responses.addMessage(Response.okay(memberId, getNickname(senderId), leaveCommand));
        }
        responses.addMessage(Response.okay(senderId, getNickname(senderId), leaveCommand));
        if (channelMap.get(channelName).isEmpty() || channelOwners.get(channelName) == senderId) {
            channelMap.remove(channelName);
            channelOwners.remove(channelName);
        }
        return responses;
    }


    // =============================================
    // == Kudos problem = Task 5: Channel Privacy ==
    // =============================================

    /*
     * This problem is worth zero points, but it will challenge your
     * ability to work with Java Collections.
     * In addition to completing the methods below, make sure to also
     * go back to createChannel and joinChannel and add all privacy-related
     * functionalities
     * If you choose not to complete this task, return null for both methods.
     */

    /**
     * This method is called when a channel's owner adds a user to that channel.
     *
     * @param inviteCommand The {@link InviteCommand} object containing all
     *                      information needed for the invite attempt
     * @return If the user joins the channel successfully as a result of the invite,
     *         a {@link ResponseSet} containing a {@link Response#names} for all
     *         people
     *         in the joined channel (including the new user).
     *         Furthermore, the {@link ResponseSet} should include a
     *         {@link Response#names}
     *         response to the newly joined user containing the names of all users
     *         in the channel (also including the newly joined user).
     *
     *         If an error occurs, use
     *         {@link Response#error} with either:
     *         (1) {@link ErrorCode#NO_SUCH_USER} if the invited user
     *         does not exist
     *         (2) {@link ErrorCode#NO_SUCH_CHANNEL} if there is no channel
     *         with the specified name
     *         (3) {@link ErrorCode#INVITE_TO_PUBLIC_CHANNEL} if the
     *         invite refers to a public channel
     *         (4) {@link ErrorCode#USER_NOT_OWNER} if the sender is not
     *         the owner of the channel
     */
    public ResponseSet inviteUser(InviteCommand inviteCommand) {
        return null;
    }

    /**
     * This method is called when a channel's owner removes a user from
     * that channel. If the user being kicked is the owner, then the
     * channel should be deleted and all its users removed.
     *
     * @param kickCommand The {@link KickCommand} object containing all
     *                    information needed for the kick attempt
     * @return If the user is successfully kicked from the channel,
     *         {@link ResponseSet} containing a {@link Response#okay} for each user
     *         in the channel, including the user who was kicked.
     *
     *         If an error occurs, use
     *         {@link Response#error} with either:
     *         (1) {@link ErrorCode#NO_SUCH_USER} if the user being kicked
     *         does not exist
     *         (2) {@link ErrorCode#NO_SUCH_CHANNEL} if there is no channel
     *         with the specified name
     *         (3) {@link ErrorCode#USER_NOT_IN_CHANNEL} if the
     *         user being kicked is not a member of the channel
     *         (4) {@link ErrorCode#USER_NOT_OWNER} if the sender is not
     *         the owner of the channel
     */
    public ResponseSet kickUser(KickCommand kickCommand) {
        return null;
    }

}
