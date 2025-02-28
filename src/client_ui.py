#!/usr/bin/env python3
import socket
import threading
import tkinter as tk
from tkinter.scrolledtext import ScrolledText

class ChatClientGUI:
    def __init__(self, master):
        self.master = master
        master.title("Chat Client")

        # Local counter for messages sent by this client
        self.local_msg_count = 0

        # --- Connection Frame ---
        self.conn_frame = tk.Frame(master)
        self.conn_frame.pack(padx=10, pady=10)

        tk.Label(self.conn_frame, text="Server IP:").grid(row=0, column=0, sticky=tk.W)
        self.entry_server = tk.Entry(self.conn_frame)
        self.entry_server.insert(0, "localhost")
        self.entry_server.grid(row=0, column=1)

        tk.Label(self.conn_frame, text="Port:").grid(row=1, column=0, sticky=tk.W)
        self.entry_port = tk.Entry(self.conn_frame)
        self.entry_port.insert(0, "12345")
        self.entry_port.grid(row=1, column=1)

        tk.Label(self.conn_frame, text="Username:").grid(row=2, column=0, sticky=tk.W)
        self.entry_username = tk.Entry(self.conn_frame)
        self.entry_username.grid(row=2, column=1)
        self.entry_username.bind("<Return>", lambda event: self.connect_to_server())

        self.connect_button = tk.Button(self.conn_frame, text="Connect", command=self.connect_to_server)
        self.connect_button.grid(row=3, column=0, columnspan=2, pady=(5,0))

        # Set focus to the username entry after the connection frame is packed
        self.master.after(100, lambda: self.entry_username.focus_set())

        # --- Mode Selection Frame (hidden initially) ---
        self.mode_frame = tk.Frame(master)
        tk.Label(self.mode_frame, text="Select Chat Mode: 1 for Public, 2 for Private").pack(padx=10, pady=(10,0))
        self.mode_entry = tk.Entry(self.mode_frame)
        self.mode_entry.pack(padx=10, pady=5)
        self.mode_entry.bind("<Return>", lambda event: self.submit_mode())
        self.mode_button = tk.Button(self.mode_frame, text="Submit Mode", command=self.submit_mode)
        self.mode_button.pack(padx=10, pady=(0,10))

        # --- Chat Frame (hidden until handshake completes) ---
        self.chat_frame = tk.Frame(master)
        self.chat_area = ScrolledText(self.chat_frame, state='disabled', width=100, height=40)
        self.chat_area.pack(padx=10, pady=5)
        self.entry_message = tk.Entry(self.chat_frame, width=70)
        self.entry_message.pack(side=tk.LEFT, padx=(10,0), pady=(0,10))
        self.entry_message.bind("<Return>", lambda event: self.send_message())
        self.send_button = tk.Button(self.chat_frame, text="Send", command=self.send_message)
        self.send_button.pack(side=tk.LEFT, padx=(5,10), pady=(0,10))

    def connect_to_server(self):
        server_ip = self.entry_server.get().strip()
        try:
            port = int(self.entry_port.get().strip())
        except ValueError:
            self.display_message("Invalid port number.")
            return

        self.username = self.entry_username.get().strip()
        if not self.username:
            self.display_message("Please enter a username.")
            return

        self.client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        try:
            self.client_socket.connect((server_ip, port))
        except Exception as e:
            self.display_message(f"Connection error: {e}")
            return

        # Hide the connection frame and show mode selection frame
        self.conn_frame.pack_forget()
        self.mode_frame.pack(padx=10, pady=10)
        self.master.after(100, lambda: self.mode_entry.focus_force())        
        
        # Start a thread to receive the initial prompt from the server (if any)
        threading.Thread(target=self.initial_prompt, daemon=True).start()

    def initial_prompt(self):
        try:
            # Optionally, receive any initial prompt from the server
            prompt = self.client_socket.recv(1024).decode().strip()
            if prompt:
                self.master.after(0, self.display_message, f"Server: {prompt}")
        except Exception as e:
            self.master.after(0, self.display_message, f"Error receiving initial prompt: {e}")

    def submit_mode(self):
        mode = self.mode_entry.get().strip()
        if mode not in ["1", "2"]:
            self.display_message("Invalid mode. Please enter 1 or 2.")
            return
        # Send the chosen mode to the server
        try:
            self.client_socket.send(mode.encode())
        except Exception as e:
            self.display_message(f"Error sending mode: {e}")
            return
        # Now start the handshake for username and further communication
        threading.Thread(target=self.handshake, daemon=True).start()
        # Hide the mode selection frame
        self.mode_frame.pack_forget()

    def handshake(self):
        try:
            # Send the username
            self.client_socket.send(self.username.encode())
            response = self.client_socket.recv(1024).decode().strip()
            if response == "taken":
                self.master.after(0, self.display_message, "Username taken. Please restart and choose another.")
                self.client_socket.close()
                return
            self.master.after(0, self.display_message, "Username accepted.")
        except Exception as e:
            self.master.after(0, self.display_message, f"Handshake error: {e}")
            return

        # Once handshake is complete, switch UI to the chat interface
        self.master.after(0, self.post_handshake_setup)

    def post_handshake_setup(self):
        self.chat_frame.pack()
        self.master.after(100, lambda: self.entry_message.focus_set())
        threading.Thread(target=self.receive_messages, daemon=True).start()

    def send_message(self):
        message = self.entry_message.get().strip()
        if message:
            try:
                # Send the message to the server.
                self.client_socket.send(message.encode())
                
                # Increment a local counter for our message numbering.
                self.local_msg_count += 1
                
                # Create a similar formatted message as the server does.
                import datetime
                timestamp = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
                # Here, we use our local message count as a stand-in for the message ID.
                local_msg = f"[{self.local_msg_count}, {self.username}, {timestamp}] {message}"
                
                # Display the message locally.
                self.display_message(local_msg)
            except Exception as e:
                self.display_message(f"Error sending message: {e}")
                return
            self.entry_message.delete(0, tk.END)
            if message == "@quit":
                self.client_socket.close()
                self.master.quit()



    def receive_messages(self):
        while True:
            try:
                message = self.client_socket.recv(1024).decode()
                if not message or message == '@q':
                    break
                self.master.after(0, self.display_message, message)
            except Exception:
                self.master.after(0, self.display_message, "Disconnected from server.")
                break

    def display_message(self, message):
        self.chat_area.config(state='normal')
        self.chat_area.insert(tk.END, message + "\n")
        self.chat_area.config(state='disabled')
        self.chat_area.yview(tk.END)

if __name__ == "__main__":
    root = tk.Tk()
    root.geometry("800x600")  # Set the window size to 800x600 pixels
    app = ChatClientGUI(root)
    root.mainloop()
