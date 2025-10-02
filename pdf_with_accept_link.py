from reportlab.pdfgen import canvas
from reportlab.lib.pagesizes import letter
from reportlab.pdfbase import pdfform
from reportlab.lib.colors import blue
import uuid
import hashlib
from datetime import datetime

class InteractivePDFGenerator:
    def __init__(self, server_url="https://your-server.com"):
        self.server_url = server_url

    def create_pdf_with_accept_button(self, content, output_path):
        """Create PDF with a unique acceptance link"""

        # Generate unique document ID
        doc_id = str(uuid.uuid4())
        doc_hash = hashlib.sha256(f"{doc_id}{datetime.now()}".encode()).hexdigest()[:16]

        # Create PDF
        c = canvas.Canvas(output_path, pagesize=letter)
        width, height = letter

        # Add content
        c.setFont("Helvetica", 12)
        y_position = height - 100
        for line in content.split('\n'):
            c.drawString(100, y_position, line)
            y_position -= 20

        # Add interactive acceptance link
        accept_url = f"{self.server_url}/accept?doc={doc_id}&token={doc_hash}"

        # Create clickable button area
        c.setFillColor(blue)
        c.rect(200, 200, 200, 50, fill=1)
        c.setFillColor("white")
        c.setFont("Helvetica-Bold", 14)
        c.drawString(260, 220, "ACCEPT")

        # Add link annotation
        c.linkURL(accept_url, (200, 200, 400, 250))

        # Add QR code for mobile acceptance
        import qrcode
        from reportlab.graphics import renderPDF
        from reportlab.graphics.shapes import Drawing
        from reportlab.graphics.barcode.qr import QrCodeWidget

        qr = QrCodeWidget(accept_url)
        qr.barWidth = 100
        qr.barHeight = 100
        qr.qrVersion = 1

        d = Drawing(100, 100)
        d.add(qr)
        renderPDF.draw(d, c, 450, 200)

        c.save()

        return doc_id, doc_hash

    def create_acceptance_server(self):
        """Flask server to handle acceptance notifications"""
        from flask import Flask, request, render_template_string
        import smtplib
        from email.message import EmailMessage

        app = Flask(__name__)

        @app.route('/accept')
        def accept_document():
            doc_id = request.args.get('doc')
            token = request.args.get('token')
            client_ip = request.remote_addr
            timestamp = datetime.now()

            # Verify token (you'd check against database)
            if self.verify_token(doc_id, token):
                # Record acceptance in database
                self.record_acceptance(doc_id, client_ip, timestamp)

                # Send notifications
                self.send_acceptance_notification(doc_id, client_ip, timestamp)

                return render_template_string('''
                    <html>
                    <body style="font-family: Arial; text-align: center; padding: 50px;">
                        <h1 style="color: green;">✓ Document Accepted</h1>
                        <p>Thank you for accepting the document.</p>
                        <p>Document ID: {{ doc_id }}</p>
                        <p>Timestamp: {{ timestamp }}</p>
                        <button onclick="window.print()">Print Confirmation</button>
                    </body>
                    </html>
                ''', doc_id=doc_id, timestamp=timestamp)
            else:
                return "Invalid or expired link", 403

        return app

    def send_acceptance_notification(self, doc_id, client_ip, timestamp):
        """Send real-time notifications"""

        # Email notification
        msg = EmailMessage()
        msg['Subject'] = f'Document {doc_id} Accepted'
        msg['From'] = 'system@company.com'
        msg['To'] = 'notifications@company.com'
        msg.set_content(f'''
        Document Acceptance Notification:
        - Document ID: {doc_id}
        - Client IP: {client_ip}
        - Timestamp: {timestamp}
        ''')

        # Send via SMTP
        with smtplib.SMTP('smtp.gmail.com', 587) as smtp:
            smtp.starttls()
            smtp.login('your-email@gmail.com', 'your-app-password')
            smtp.send_message(msg)

        # Push notification via websocket
        import socketio
        sio = socketio.Client()
        sio.connect('http://your-notification-server.com')
        sio.emit('document_accepted', {
            'doc_id': doc_id,
            'timestamp': str(timestamp),
            'client_ip': client_ip
        })

        # SMS notification (using Twilio)
        from twilio.rest import Client
        twilio_client = Client('account_sid', 'auth_token')
        twilio_client.messages.create(
            body=f"Document {doc_id} accepted at {timestamp}",
            from_='+1234567890',
            to='+0987654321'
        )

# Usage
generator = InteractivePDFGenerator()
doc_id, token = generator.create_pdf_with_accept_button(
    "This is the agreement content.\nPlease review and accept.",
    "agreement.pdf"
)
print(f"PDF created with tracking: {doc_id}")

# Run the server
app = generator.create_acceptance_server()
# app.run(host='0.0.0.0', port=5000)