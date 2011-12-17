# Simple static web server for Fiji tests

import os, threading, time
from BaseHTTPServer import BaseHTTPRequestHandler, HTTPServer

defaultHTML = '''<html>
    <head>
        <title>Fiji Test Server</title>
    </head>
    <body>
        <h1>Fiji Test Server</h1>
        Congratulations, it works.
    </body>
</html>'''

class MyHandler(BaseHTTPRequestHandler):
    def sendHeaders(self, path):
        if path.endswith('.html'):
            self.file = None
            self.lastModified = time.time()
            self.length = len(defaultHTML)
            self.type = 'text/html'
        else:
	    while self.path.startswith('.'):
                self.path = self.path[1:]
            path = self.server.directory + '/' + self.path
            print path
            self.file = open(path)
            print 'opened'
            st = os.stat(path)
            self.lastModified = st.st_mtime
            self.length = st.st_size
            self.type = 'application/octet-stream'

        self.send_response(200)
        self.send_header('Server', 'Fiji Test Server')
        self.send_header('Last-Modified', time.ctime(self.lastModified))
        self.send_header('Content-Length', self.length)
        self.send_header('Content-Type', self.type)
        self.end_headers()

    def do_HEAD(self):
        try:
            self.sendHeaders(self.path)
            if self.file != None:
                self.file.close()
                self.file = None
        except IOError:
            self.send_error(404,'File Not Found: %s' % self.path)

    def do_GET(self):
        try:
            self.sendHeaders(self.path)
            if self.file == None:
                self.wfile.write(defaultHTML)
                return
            self.wfile.write(self.file.read())
            self.file.close()
            self.file = None
        except IOError:
            self.send_error(404,'File Not Found: %s' % self.path)

def startHTTP(directory):
    server = HTTPServer(('', 12679), MyHandler)
    server.directory = directory
    threading.Thread(target=server.serve_forever).start()
    return server

def stopHTTP(server):
    server.shutdown()

if __name__ == '__main__':
    if len(sys.argv) > 1:
        directory = sys.argv[1]
    else:
        from java.lang import System
        directory = System.getProperty('ij.dir') + '/tests'
    server = startHTTP(directory)
    print 'started httpserver...'
