# TODO

- [ ] Configure Amazon SES SMTP for account email verification. Set `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, and `APP_PUBLIC_URL` as environment variables before enabling the feature.
- [ ] Email verification is temporarily disabled so account registration can continue without SMTP. Re-enable `EMAIL_VERIFICATION_ENABLED` after Amazon SES SMTP is configured and tested.
