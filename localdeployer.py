import threading
import os

file_name = "A7.jar"
        
def deploy(port):
    os.system("java -Xmx64m -jar %s %s" % (file_name, port))

server_list = open("src/util/server.list", "r").read().split("\n")
for server in server_list:
    if not server or server[0] == "#":
        continue
    print server
    ip = server.split(":")[0]
    port = int(server.split(":")[1])

    threading.Thread(target=deploy, args=(port,)).start()
