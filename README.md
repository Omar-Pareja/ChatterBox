# Relay - Multi-Channel Chat Server

**Relay** is a real-time chat server supporting multiple channels, user management, and structured command processing. Built using **TreeMap**, **TreeSet**, and **LinkedList**, Relay ensures efficient data storage and retrieval for seamless communication.

---

## Features

- **Multi-Channel Messaging** – Users can create, join, and leave chat channels.
- **User Registration & Nicknames** – Automatic nickname assignment with `/nick` command support.
- **Message Broadcasting** – Users can send messages to all members in a channel.
- **Command-Based Interaction** – Supports structured commands for chat management.
- **Channel Ownership** – Channels have designated owners with administrative control.
- **Encapsulation & Modular Design** – Server logic is separated into well-defined components.

---

## Installation

Clone the repository and navigate to the project directory:

git clone https://github.com/yourusername/relay-chat-server.git
cd relay-chat-server

Usage
1. Compile the Code
sh
Copy
Edit
javac -d bin src/**/*.java
2. Run the Server
sh
Copy
Edit
java -cp bin ServerMain
3. Connect Clients
Use a terminal-based client like Telnet:

sh
Copy
Edit
telnet localhost 12345

Supported Commands
Command	Description
/nick <name>	Change user nickname.
/join <channel>	Join an existing channel (or create it).
/leave <channel>	Leave a channel.
/msg <channel> <msg>	Send message to a channel.
/quit	Disconnect from the server.
Server Functionality
User & Channel Management
Assigns unique nicknames upon user registration.
Users can be deregistered, removing them from channels.
Channel owners control their channels, and removing an owner deletes the channel.
Command Processing
Error handling for invalid user operations:
Invalid nicknames are rejected.
Users cannot join nonexistent channels.
Owners have control over channel deletion.
Channels track their users, ensuring correct message delivery.
Efficient Data Structures
TreeMap for ordered user tracking.
TreeSet ensures unique and sorted nicknames.
LinkedList supports efficient message handling.
Development & Testing
Unit Tests: JUnit tests verify correct server operations (ServerModelTest.java).
Edge Cases: Tests include invalid names, duplicate names, non-existent channels, and deregistering owners.
Logging: Debugging outputs track user and channel actions.
Future Enhancements
Implement private channels (invite-only access).
Add user kicking & invitations for channel moderation.
Expand server logging for better debugging & analytics.
