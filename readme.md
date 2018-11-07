# instagram-downloader

## What is it?
JavaFX based application for downloading media (JPG, MPG) from Instagram with the public API
(so only public media is downloadable).

The following URI formats are supported:

- a single image or video: https://www.instagram.com/p/BcK0wlABWaQ/
- an account: https://www.instagram.com/unsereoebb/
  Downloads n pages from this account (12 media files per page)

The instagram filenames are kept, a file is never overridden. So it is possible to
fetch only the new files of an account by simply downloading it again.

Following a screenshot of the application:

![Screenshot](src/doc/screenshot.png?raw=true)


## History
- 2018.11.07 Update to the last Instagram change. I followed mainly this
  [blog entry](https://www.diggernaut.com/blog/how-to-scrape-pages-infinite-scroll-extracting-data-from-instagram/)
- 2018.04.10 Instagram turned off the pagination support of the prior used API.
  So I changed the API as recommended in 
  [Instagram ?__a=1&max_id=<end_cursor> isn't working for public user feeds](https://stackoverflow.com/questions/49265013/instagram-a-1max-id-end-cursor-isnt-working-for-public-user-feeds).


## Credits
- Icon taken from [IconArchive]()http://www.iconarchive.com/show/papirus-apps-icons-by-papirus-team/instagram-icon.html)
- [blog entry](https://www.diggernaut.com/blog/how-to-scrape-pages-infinite-scroll-extracting-data-from-instagram/)
- [Stack overflow question 1](https://stackoverflow.com/questions/49265013/instagram-a-1max-id-end-cursor-isnt-working-for-public-user-feeds).
- [Stack overflow question 2](https://stackoverflow.com/questions/49786980/how-to-perform-unauthenticated-instagram-web-scraping-in-response-to-recent-priv).
- [Gist 1](https://gist.github.com/winder/a97cc4d9480d4f12620f4602369d61f3)
- [Gist 2](https://gist.github.com/ketankr9/6e48c6c205907e6ae35ef789e7a03634)


## Links to similar software (mostly written in Python)
- [instagram-scrape](https://github.com/rarcega/instagram-scraper) for Instagram access via public and private api
- [InstaLooter](https://github.com/althonos/InstaLooter) for Instagram access via public api
- [instagram_private_api](https://github.com/ping/instagram_private_api) for Instagram access via private api