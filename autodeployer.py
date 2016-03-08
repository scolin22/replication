import paramiko
import scp
import socket
import threading
import time

name = "javadreamteam"
if not name:
    print "GIMME NAME"
    exit()
port = "45111"
if not port:
    print "GIMME PORT"
    exit()
username = "ubc_cpen431_1"
file_name = "A5.jar"
remote_directory = "~/" + name + "/"
remote_file_name = remote_directory + file_name

terminate_command = "screen -X -S %s quit" % name
start_command = "screen -d -m -S %s; screen -S %s -p 0 -X stuff $'java -Xmx64m -jar %s %s\n'" % (name, name, remote_file_name, port)

lock = threading.Lock()
success_count = 0
fail_count = 0

def execAndWait(cmd, ssh):
    max_wait = 3;
    ssh_stdin, ssh_stdout, ssh_stderr = ssh.exec_command(cmd, timeout=15)
    while not ssh_stdout.channel.exit_status_ready() and max_wait > 0:
        max_wait -= 1
        time.sleep(5)
        print "sleeping"
        
def deploy(ip):
    global fail_count
    global success_count
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    try:
        ssh.connect(ip, username=username, timeout=15)

        execAndWait("mkdir " + remote_directory, ssh)
        
        execAndWait(terminate_command, ssh)

        scpC = scp.SCPClient(ssh.get_transport())
        scpC.put(file_name, remote_directory)

        execAndWait(start_command, ssh)
        
        print ip + " SUCCESS"
        lock.acquire()
        success_count += 1
        print success_count, fail_count
        lock.release()
    except scp.SCPException:
        print ip + " SCP Time Out"
        lock.acquire()
        fail_count += 1
        print success_count, fail_count
        lock.release()
    except paramiko.AuthenticationException:
        print ip + " Auth Error"
        lock.acquire()
        fail_count += 1
        print success_count, fail_count
        lock.release()
    except socket.error:
        print ip + " SSH Timed Out"
        lock.acquire()
        fail_count += 1
        print success_count, fail_count
        lock.release()
    except paramiko.SSHException:
        print ip + " SSH Exception"
        lock.acquire()
        fail_count += 1
        print success_count, fail_count
        lock.release()

server_list = open("src/util/server.list", "r").read().split("\n")
for server in server_list:
    if not server or server[0] == "#":
        continue
    print server
    ip = server.split(":")[0]
    #port = int(server.split(":")[1])

    threading.Thread(target=deploy, args=(ip,)).start()
