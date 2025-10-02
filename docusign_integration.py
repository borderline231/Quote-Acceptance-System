import requests
from docusign_esign import ApiClient, EnvelopesApi, Document, Signer, Recipients, EnvelopeDefinition

class DocumentAcceptanceSystem:
    def __init__(self, api_key, account_id):
        self.api_key = api_key
        self.account_id = account_id
        self.api_client = ApiClient()

    def create_document_with_accept_button(self, pdf_path, recipient_email):
        """Create a document with acceptance tracking"""

        # Create envelope with document
        envelope_definition = EnvelopeDefinition(
            email_subject="Please review and accept",
            documents=[Document(
                document_base64=self.get_base64_pdf(pdf_path),
                name="Agreement",
                file_extension="pdf",
                document_id="1"
            )],
            recipients=Recipients(
                signers=[Signer(
                    email=recipient_email,
                    name="Client",
                    recipient_id="1",
                    routing_order="1",
                    tabs={
                        "approveTabs": [{
                            "xPosition": "200",
                            "yPosition": "500",
                            "documentId": "1",
                            "pageNumber": "1",
                            "buttonText": "I Accept"
                        }]
                    }
                )]
            ),
            status="sent",
            # Webhook for real-time notifications
            event_notification={
                "url": "https://your-server.com/webhook/docusign",
                "events": ["recipient-completed", "envelope-completed"]
            }
        )

        # Send the envelope
        envelopes_api = EnvelopesApi(self.api_client)
        results = envelopes_api.create_envelope(self.account_id, envelope_definition=envelope_definition)

        return results.envelope_id

    def setup_webhook_listener(self):
        """Flask endpoint to receive notifications"""
        from flask import Flask, request, jsonify

        app = Flask(__name__)

        @app.route('/webhook/docusign', methods=['POST'])
        def handle_docusign_webhook():
            data = request.json

            if data.get('event') == 'recipient-completed':
                # Client accepted the document
                self.send_notification(
                    f"Client {data['recipient_email']} accepted document {data['envelope_id']}"
                )

            return jsonify({"status": "received"}), 200

        return app

    def send_notification(self, message):
        """Send notification to your system"""
        # Email notification
        requests.post("https://api.sendgrid.com/v3/mail/send",
            headers={"Authorization": f"Bearer {self.sendgrid_key}"},
            json={
                "personalizations": [{"to": [{"email": "admin@company.com"}]}],
                "from": {"email": "system@company.com"},
                "subject": "Document Accepted",
                "content": [{"type": "text/plain", "value": message}]
            }
        )

        # Slack notification
        requests.post("https://hooks.slack.com/services/YOUR/WEBHOOK/URL",
            json={"text": message}
        )

        # Database update
        # Update your database to record the acceptance

# Usage
system = DocumentAcceptanceSystem(api_key="your-key", account_id="your-account")
envelope_id = system.create_document_with_accept_button("contract.pdf", "client@email.com")
print(f"Document sent with tracking ID: {envelope_id}")