import java.util.*;


public final class ServerModel {
    private final Map<Integer, String> userMap;
    private final Map<String, Set<Integer>> channelMap;
    private final Map<String, Integer> channelOwners;

    
    public ServerModel() {
        userMap = new TreeMap<>();
        channelMap = new TreeMap<>();
        channelOwners = new TreeMap<>();
    }

   
    public boolean existingNickname(String nickname) {
        return userMap.containsValue(nickname);
    }

    
    public boolean existingUserId(Integer userId) {
        return userMap.containsKey(userId);
    }

  
    public int getUserId(String userNickname) {
        for (Map.Entry<Integer, String> entry : userMap.entrySet()) {
            if (entry.getValue().equals(userNickname)) {
                return entry.getKey();
            }
        }
        throw new IllegalArgumentException("Unknown nickname: " + userNickname);
    }


    public String getNickname(int userId) {
        if (userMap.containsKey(userId)) {
            return userMap.get(userId);
        }
        throw new IllegalArgumentException("User Id " + userId + " has no associate nickname.");
    }

  
    public Collection<String> getRegisteredUsers() {
        return new ArrayList<>(userMap.values());
    }

    public Collection<String> getChannels() {
        return new ArrayList<>(channelMap.keySet());
    }

 
    public Collection<Integer> getUserIdsInChannel(String channelName) {
        if (!channelMap.containsKey(channelName)) {
            throw new IllegalArgumentException("Unknown channel: " + channelName);
        }
        return new HashSet<>(channelMap.get(channelName));
    }

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


    public String getOwner(String channelName) {
        if (!channelOwners.containsKey(channelName)) {
            throw new IllegalArgumentException("Unknown channel: " + channelName);
        }
        int ownerId = channelOwners.get(channelName);
        return getNickname(ownerId);
    }


    public ResponseSet registerUser(int userId) {
        String userNickname = generateUniqueNickname();
        userMap.put(userId, userNickname);
        return ResponseSet.singleMessage(Response.connected(userId, userNickname));
    }

    private String generateUniqueNickname() {
        int suffix = 0;
        String userNickname;
        Collection<String> existingUsers = getRegisteredUsers();
        do {
            userNickname = "User" + suffix++;
        } while (existingUsers.contains(userNickname));
        return userNickname;
    }

   
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



    public ResponseSet inviteUser(InviteCommand inviteCommand) {
        return null;
    }


    public ResponseSet kickUser(KickCommand kickCommand) {
        return null;
    }

}
