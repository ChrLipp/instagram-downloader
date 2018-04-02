JavaFX based application for downloading media (JPG, MPG) from Instagram.
There is no login implemented, so only public media is downloadable.

The following URI formats are supported:

- a single image or video: https://www.instagram.com/p/BcK0wlABWaQ/
- an account: https://www.instagram.com/unsereoebb/
  Downloads n pages from this account (12 media files per page)

Instagram turned of the pagination support of the prior used API.
So I changed the API as recommended in 
[Instagram ?__a=1&max_id=<end_cursor> isn't working for public user feeds](https://stackoverflow.com/questions/49265013/instagram-a-1max-id-end-cursor-isnt-working-for-public-user-feeds).


![Screenshot]()src/doc/screenshot.png)
