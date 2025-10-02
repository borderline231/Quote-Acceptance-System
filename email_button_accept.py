def send_email_with_accept_button(client_email, doc_id):
    """Send email with a single accept button - simplest possible approach"""

    html_content = f'''
    <html>
    <body style="font-family: Arial, sans-serif; padding: 20px; max-width: 600px; margin: 0 auto;">
        <h2>Agreement Ready for Your Acceptance</h2>
        <p>Please review the attached PDF document.</p>
        <p>When ready, click the button below to accept:</p>

        <div style="text-align: center; margin: 40px 0;">
            <a href="https://your-domain.com/accept?id={doc_id}"
               style="background-color: #10b981;
                      color: white;
                      padding: 15px 50px;
                      text-decoration: none;
                      border-radius: 5px;
                      font-size: 18px;
                      font-weight: bold;
                      display: inline-block;">
                ✓ I ACCEPT
            </a>
        </div>

        <p style="color: #666; font-size: 12px;">
            This link expires in 7 days. Document ID: {doc_id[:8]}
        </p>
    </body>
    </html>
    '''

    # Send via your preferred email service
    import smtplib
    from email.mime.multipart import MIMEMultipart
    from email.mime.text import MIMEText
    from email.mime.application import MIMEApplication

    msg = MIMEMultipart()
    msg['Subject'] = 'Agreement Ready for Acceptance'
    msg['From'] = 'noreply@company.com'
    msg['To'] = client_email

    # HTML body
    msg.attach(MIMEText(html_content, 'html'))

    # Attach PDF
    with open('agreement.pdf', 'rb') as f:
        pdf_attachment = MIMEApplication(f.read(), _subtype='pdf')
        pdf_attachment.add_header('Content-Disposition', 'attachment', filename='agreement.pdf')
        msg.attach(pdf_attachment)

    # Send
    with smtplib.SMTP('smtp.gmail.com', 587) as server:
        server.starttls()
        server.login('your-email@gmail.com', 'your-app-password')
        server.send_message(msg)

    print(f"Email sent with one-click acceptance to {client_email}")