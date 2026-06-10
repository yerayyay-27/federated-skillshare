# Skillshare — User Manual

Software Engineering project — Corso di Laurea in Informatica per il Management
University of Bologna, A.Y. 2025/2026

Skillshare is a peer-to-peer platform for exchanging skills between users. Instead of buying courses, every member is both a student and a teacher: you offer something you can teach and, in return, ask for something you would like to learn. This guide explains, step by step, how to use each feature of the application.

---

## 1. Getting started

Open the application in your web browser at:

```
http://localhost:8080/
```

The first screen is the **Login** page. If you do not have an account yet, you can create one from here.

You can try the application straight away with the demo account:

- **Email:** `test@unibo.it`
- **Password:** `1234`

---

## 2. Creating an account (registration)

1. On the Login page, click **"Don't have an account? Sign up"**.
2. Fill in the form:
   - **Username** — the name other users will see.
   - **Email** — used to sign in.
   - **Password** — at least 4 characters.
3. Click **Create account**.

If everything is correct, you will see a confirmation message and be taken back to the Login page, where you can sign in with your new credentials. If something is wrong (for example the email is already in use, or the password is too short), a message will explain the problem.

---

## 3. Signing in (login)

1. On the Login page, enter your **email** and **password**.
2. Click **Sign in**.

If the credentials are correct, you arrive at the **Home** page. If they are not, a message tells you that the email or password is incorrect.

---

## 4. The Home page

The Home page is your starting point once signed in. It greets you by name and gives you access to the main areas of the application:

- **Open marketplace** — browse and publish skill exchange announcements.
- **My profile** — edit your personal information and photo.
- **My exchange requests** — manage the requests you have sent and received.
- **My reputation** — see your average rating and the reviews you have received.
- **Sign out** — return to the Login page.

---

## 5. Your profile

From the Home page, click **My profile**. Here you can:

- **Upload a profile photo** — click the file selector, choose an image from your computer (use a small image), then click **Upload photo**. Your picture appears as a preview.
- **Write a bio** — a short description of yourself.
- **Add skill tags** — a comma-separated list of the things you can teach or are interested in, for example: `Java, Guitar, Cooking`.

Click **Save changes** to store your bio and tags. Use **Back to home** to return.

---

## 6. The Skill Marketplace

From the Home page, click **Open marketplace**. The marketplace shows all the active announcements published by users.

### 6.1 Browsing and searching

- All active announcements are listed, each showing the offered skill, the requested skill, a description, the availability, and who published it.
- To find something specific, type in the **search box** and click **Search**. The search looks through the offered skill, the requested skill and the description. Leaving the box empty and searching shows all announcements again.

### 6.2 Publishing an announcement

1. Click **Create announcement**.
2. Fill in the fields: what you offer to teach, what you would like to receive in return, a description, and your availability.
3. Submit the form. Your announcement now appears in the marketplace for other users.

### 6.3 Managing your own announcements

On announcements that you published, you will see actions to **edit** or **remove** them, so you can keep your listings up to date. These actions only appear on your own announcements.

---

## 7. Requesting an exchange

When you find an announcement from another user that interests you:

1. Click **Request exchange** on that announcement.
2. Optionally, write a short message for the owner.
3. The request is sent. The owner will see it among their received requests.

You cannot request your own announcement, and you cannot send a second request for an announcement you have already requested.

---

## 8. Managing exchange requests

From the Home page, click **My exchange requests**. The page is divided in two sections:

- **Received** — requests other users have sent you. For each pending request you can **Accept** or **Reject** it.
- **Sent** — requests you have sent to others, with their current status (pending, accepted or rejected).

When a request is **accepted**, two new actions become available to both participants on that request: **Open chat** and **Leave a review** (see below).

---

## 9. Internal chat

Once an exchange request has been accepted, both participants can open a private chat to arrange the practical details (when and where to meet, links, etc.).

1. In **My exchange requests**, find an accepted request and click **Open chat**.
2. Type your message and click **Send**.
3. Click **Refresh** to load new messages sent by the other person.

Only the two participants of an accepted exchange can read or write in that chat.

---

## 10. Reviews and reputation

After an accepted exchange, each participant can leave one review for the other.

### 10.1 Leaving a review

1. In **My exchange requests**, on an accepted request, click **Leave a review**.
2. Choose a **rating** from 1 to 5 stars and, optionally, write a **comment**.
3. Click **Submit review**.

You can leave only one review per exchange. If you have already reviewed it (or the review is not allowed for another reason), the page will explain why instead of letting you submit again.

### 10.2 Viewing reputation

From the Home page, click **My reputation** to see your **average rating**, the **number of reviews** you have received, and the individual reviews left by other users. This public reputation helps build trust between members of the community.

---

## 11. Signing out

To end your session, click **Sign out** on the Home page. You will return to the Login page.
