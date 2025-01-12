# Clojure-mail

A clojure library for parsing, downloading and reading email from IMAP servers.

## Original package by owainlewis

![](https://travis-ci.org/owainlewis/clojure-mail.svg?branch=master)

[![Clojars Project](http://clojars.org/io.forward/clojure-mail/latest-version.svg#)](http://clojars.org/io.forward/clojure-mail)

## Add this dependency to your project

I have not published my fork on Clojars (at least not yet). A `deps.edn` coordinate looks like this:

```clojure
{:deps {org.clojure/clojure {:mvn/version "1.10.3"}
        com.github.claj/clojure-mail {:git/tag "v2.0.0-alpha-1"
                                      :git/sha "77dfdfa5b92e43e8ff015d3f0c0244e6d7098d13"}}}
```

## Why fork clojure-mail?

I forked clojure-mail on 1 jan 2022 because

- there was no activity in the original project for quite some time
- the README was confusing
- the described authentication methods for gmail accounts was outdated
- the original dependencies (Jakarta Mail) had made quite large changes

I also suspected there where problems with SSL/TLS in the older Jakarta Mail packages since there has been API changes in the more recent versions of the JVM, but I was probably wrong about this.

## QuickStart with Protonmail Bridge

Example confirmed working 2021-01-01.

[Protonmail](https://protonmail.com/) is a secure mail service whose business model is to sell well working email services, not targeted advertisments. Reasonable free tiers exists.

To make ProtonMail accounts work with locally running mail clients (like Thunderbird etc), Protonmail asks you to install [Protonmail Bridge](https://protonmail.com/bridge/), which serves acts as a customized proxy to access the mailservers. Given the seemingly endless security problems that can occur with IMAP/SMTP-configurations I think Protonmails solution is a usable one that gives me higher confidence in the security of the system.

It's possible to get Clojure-mail to work with Protonmail Bridge like this:

```clojure
(ns myproject.core
  (:require [clojure-mail.core :as m]
            [clojure-mail.message :refer [read-message]]))

;; these details can be found in
;; Protonmail Bridge -> Account name -> Mailbox configuration

(def hostname "127.0.0.1")
(def port 1143) ;; could differ on your machine!
(def username "your-username@protonmail.com")

;;; NOTE: This is a generated password used only locally
;;; DO NOT put your precious protonmail password in your clojure code.
(def protonmail-bridge-password "password-generated-by-protonmail-bridge")


(def the-store (m/store "imap" [hostname port] username protonmail-bridge-password))

;; inbox returns a list of messages
(def inbox-messages (m/inbox the-store))

;; how many messages in my inbox?
(count inbox-messages)

;; define a clojure data structure of the first mail in the inbox.
(def latest (read-message (first inbox-messages))

(:subject latest)

(:keys latest)
;; => (:id :to :cc :bcc :from :sender :subject :date-sent :date-recieved :multipart? :content-type :body :headers)
```

## Getting started with Gmail + OAuth2 (unconfirmed after fork)

The first thing we need is a mail store which acts as a gateway to our IMAP account.

```clojure
(def store (m/store "imap.gmail.com" "user@gmail.com" "password"))
```

You can also authenticate using an Oauth token.

```clojure
(def store (m/xoauth2-store "imap.gmail.com" "user@gmail.com" "user-oauth-token"))
```

Now we can fetch email messages easily.

```clojure
(def my-inbox-messages (take 5 (m/all-messages store "inbox")))

(def first-message (first my-inbox-messages))

(message/subject first-message) ;; => "Hi! Here are your new links from the weekend"
```

Note that the messages returned are Java mail message objects.


## Reading email messages

```clojure

(def javamail-message (first inbox-messages))

;; To read the entire message as a clojure map
(def message (m/read-message javamail-message))

;; There are also individual methods available in the message namespace. I.e to read the subject
;; of a jakarta.mail message
(message/subject javamail-message)

;; You can also select only the fields you require
(def message (m/read-message javamail-message :fields [:id :to :subject]))

```

An email message returned as a Clojure map from read-message looks something like this:

```clojure

{:subject "Re: Presents for Dale's baby",
 :from {:address "<someone@aol.com>" :name "Someone"}
 :date-recieved "Tue Mar 11 12:54:41 GMT 2014",
 :to ({:address "owain@owainlewis.com" :name "Owain Lewis"}),
 :cc (),
 :bcc (),
 :multipart? true,
 :content-type "multipart/ALTERNATIVE",
 :sender {:address "<someone@aol.com>" :name "Someone"},
 :date-sent #inst "2015-10-23T12:19:33.838-00:00"
 :date-received #inst "2015-10-23T12:19:33.838-00:00"
 :body [{:content-type "text/plain" :body "..."}
        {:content-type "text/html"  :body "..."}]
 :headers {"Subject" "Re: Presents for Dale's baby" .......}

```

## Searching your inbox (unconfirmed after fork)

You can easily search your inbox for messages

```clojure
(def s (gen-store "user@gmail.com" "password"))
(def results (m/search-inbox s "projects"))
(def results (m/search-inbox s [:body "projects" :subject "projects"]))
(def results (m/search-inbox s :body "projects" :received-before :yesterday))
(def results (m/search-inbox s :body "projects" :from "john@example.com"))

(->> results first subject) ;; => "Open Source Customisation Projects"
```

## Parser

HTML emails are evil. There is a simple HTML -> Plain text parser provided if you need to
do any machine learning type processing on email messages.

```clojure
(require '[clojure-mail.parser :as mp])

(mp/html->text "<h1>I HATE HTML EMAILS</h1>")

;; => "I HATE HTML EMAILS"

```

## Watching a folder (unconfirmed after fork)

Some IMAP servers allow the use of the IDLE command to receive push notifications when a folder changes.

```clojure
(require '[clojure-mail.events :as events])

;; Create a manager and start listening to the inbox, printing the subject of new messages
(def manager
  (let [s (get-session "imaps")
        gstore (store "imaps" s "imap.gmail.com" "me@gmail.com" "mypassword")
        folder (open-folder gstore "inbox" :readonly)
        im (events/new-idle-manager s)]
    (events/add-message-count-listener (fn [e]
                                  (prn "added" (->> e
                                                    :messages
                                                    (map read-message)
                                                    (map :subject))))
                                #(prn "removed" %)
                                folder
                                im)
    im))
;; now we wait...

"added" ("added" ("test!")
"added" ("added" ("another test!")

;; we received some messages and printed them, now we can stop the manager as we are finished
(events/stop manager)

```

## Reading emails from disk

Clojure mail can be used to parse existing email messages from file. Take a look in dev-resources/emails to see some example messages. To read one of these messages we can do something like this


```clojure
(require '[clojure-mail.core :as m])

(def message (m/file->message "test/clojure_mail/fixtures/25"))

(m/read-message message)

;; =>
;; {:subject "Request to share ContractsBuilder",
;; :from nil, :date-recieved nil,
;; :to "zaphrauk@gmail.com",
;; :multipart? true,
;; :content-type "multipart/alternative; boundary=90e6ba1efefc44ffe804a5e76c56",
;; :sender nil,
;; :date-sent "Fri Jun 17 13:21:19 BST 2011" ..............

```

## License

Copyright © 2017 Owain Lewis

Distributed under the Eclipse Public License, the same as Clojure.
