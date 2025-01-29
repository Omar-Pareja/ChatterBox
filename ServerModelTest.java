

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;

public class ServerModelTest {
    private ServerModel model;

   
    @BeforeEach
    public void setUp() {
      
        model = new ServerModel();
    }


   
    @Test
    public void testInvalidNickname() {
      
        model.registerUser(0);

        
        Command command = new NicknameCommand(0, "!nv@l!d!");

   
        ResponseSet actual = command.updateServerModel(model);

        ResponseSet expected = ResponseSet.singleMessage(
                Response.error(
                        command, ErrorCode.INVALID_NAME
                )
        );

  
        assertEquals(expected, actual, "error response for invalid nickname");

       
        Collection<String> users = model.getRegisteredUsers();

     
        assertEquals(1, users.size(), "Number of registered users");

    
        assertTrue(users.contains("User0"), "Old nickname still registered");

      
        assertEquals(
                "User0", model.getNickname(0),
                "User with id 0 nickname unchanged"
        );
    }


    @Test
    public void testRegisterSingleUserResponseSet() {
        
        ResponseSet expected = ResponseSet.singleMessage(Response.connected(0, "User0"));
       
        assertEquals(expected, model.registerUser(0), "User0 registered");

        ResponseSet expected2 = new ResponseSet();
      
        expected2.addMessage(Response.connected(0, "User0"));
      
        assertEquals(expected, expected2, "Different ways of creating a ResponseSet");
       
        Collection<String> registeredUsers = model.getRegisteredUsers();
        assertEquals(1, registeredUsers.size(), "Num. registered users");

        assertTrue(registeredUsers.contains("User0"), "User0 is registered");
    }

   
    @Test
    public void testRegisterUserWithMultipleUsers() {
        model.registerUser(0);
        model.registerUser(1);
        model.registerUser(2);
        assertTrue(model.existingUserId(0), "User ID 0 should exist after registration");
        assertTrue(model.existingUserId(1), "User ID 1 should exist after registration");
        assertTrue(model.existingUserId(2), "User ID 2 should exist after registration");
        assertEquals(3, model.getRegisteredUsers().size(), "Three users should be registered");
    }

    @Test
    public void testDeregisterUserNotRegistered() {
        assertThrows(IllegalArgumentException.class, () -> model.deregisterUser(99),
                "Deregistering a "
                +
                "non-existent user should throw an exception");
    }

    @Test
    public void testGetUserIdWithNonexistentNickname() {
        model.registerUser(0);
        assertThrows(IllegalArgumentException.class, () -> model.getUserId("NonexistentUser"), ""
                +
                "Getting user ID for a nonexistent nickname should throw an exception");
    }

    @Test
    public void testGetUsersInNonExistentChannel() {
        assertThrows(
                IllegalArgumentException.class,
                () -> model.getUserNicknamesInChannel("nonexistent"),
                "Nonexistent channel should throw exception"
        );
    }

    @Test
    public void testGetOwnerInvalidChannelName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> model.getOwner("invalid channel name"),
                "Invalid channel name should throw exception"
        );
    }

    @Test
    public void testAllEmptyStateQueries() {
        assertTrue(model.getRegisteredUsers().isEmpty(), "No registered users");
        assertTrue(model.getChannels().isEmpty(), "No channels");
        assertThrows(IllegalArgumentException.class, () -> model.getUserId("NonExistentUser"));
        assertThrows(IllegalArgumentException.class, () -> model.getNickname(123));
    }

    @Test
    public void testRegisterAndDeregisterMultipleUsers() {
        model.registerUser(0);
        model.registerUser(1);
        model.registerUser(2);
        model.deregisterUser(1);
        assertFalse(model.existingUserId(1), "User ID 1 should not exist after deregistration");
        assertEquals(2, model.getRegisteredUsers().size(), "Two users should be registered after" +
                " deregistering one");
    }

    @Test
    public void testCreateChannelWithInvalidName() {
        model.registerUser(0);
        Command create = new CreateCommand(0, "invalid@name!", false);

        ResponseSet expected = ResponseSet.singleMessage(
                Response.error(create, ErrorCode.INVALID_NAME)
        );
        assertEquals(expected, create.updateServerModel(model), "Invalid channel name");
        assertFalse(model.getChannels().contains("invalid@name!"), "Channel should not be created");
    }

    @Test
    public void testJoinChannelNonExistent() {
        model.registerUser(0);
        Command join = new JoinCommand(0, "nonexistent");

        ResponseSet expected = ResponseSet.singleMessage(
                Response.error(join, ErrorCode.NO_SUCH_CHANNEL)
        );
        assertEquals(expected, join.updateServerModel(model),
                "Attempt to join nonexistent channel");
    }

    @Test
    public void testQueryUsersPostDeregister() {
        model.registerUser(0);
        model.deregisterUser(0);
        assertThrows(IllegalArgumentException.class, () ->
                model.getNickname(0), "Querying nickname " +
                "for deregistered user should fail");
    }


    @Test
    public void testGetOwnerEmptyChannelName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> model.getOwner(""),
                "Empty channel name should throw exception"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> model.getOwner(" "),
                "Whitespace channel name should throw exception"
        );
    }


    @Test
    public void testLeaveChannelNonMember() {
        model.registerUser(0);
        model.registerUser(1);
        Command create = new CreateCommand(0, "java", false);
        create.updateServerModel(model);

        Command leave = new LeaveCommand(1, "java");

        ResponseSet expected = ResponseSet.singleMessage(
                Response.error(leave, ErrorCode.USER_NOT_IN_CHANNEL)
        );
        assertEquals(expected, leave.updateServerModel(model),
                "Non-member attempts to leave channel");
    }
    @Test
    public void testDeregisterOwnerRemovesMultipleOwnedChannels() {

        model.registerUser(0); 
        model.registerUser(1); 


        Command createChannel1 = new CreateCommand(0, "java", false);
        Command createChannel2 = new CreateCommand(0, "python", false);
        createChannel1.updateServerModel(model);
        createChannel2.updateServerModel(model);

        Command joinChannel = new JoinCommand(1, "java");
        joinChannel.updateServerModel(model);

        assertTrue(model.getChannels().contains("java"), "Channel" +
                " 'java' should exist before deregistration");
        assertTrue(model.getChannels().contains("python"), "Channel " +
                "'python' should exist before deregistration");

        ResponseSet expected = new ResponseSet();
        expected.addMessage(Response.disconnected(1, "User0"));
        ResponseSet actual = model.deregisterUser(0);
        assertEquals(expected, actual, "Deregistering owner " +
                "should broadcast disconnection to other members");

        assertFalse(model.getChannels().contains("java"), "Channel" +
                " 'java' should be removed after owner deregistration");
        assertFalse(model.getChannels().contains("python"), "Channel " +
                "'python' should be removed after owner deregistration");
    }
}
