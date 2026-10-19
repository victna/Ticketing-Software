package com.ticketing.photo;

import com.ticketing.pass.Pass;
import com.ticketing.pass.PassService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.util.Optional;

/** Java version of server.js: the page a phone/tablet opens to take a pass photo. */
@RestController
public class PhotoController {

    private final PassService passes;
    private final PhotoService photos;

    public PhotoController(PassService passes, PhotoService photos) {
        this.passes = passes;
        this.photos = photos;
    }

    private static String esc(String text) {
        return HtmlUtils.htmlEscape(text);
    }

    private static String page(String title, String body) {
        return """
                <!doctype html>
                <html><head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>__TITLE__</title>
                <style>
                  body { font-family: sans-serif; max-width: 480px; margin: auto; padding: 16px; text-align: center; }
                  .btn { display: block; width: 100%; box-sizing: border-box; padding: 16px; margin: 12px 0;
                         font-size: 18px; border: 0; border-radius: 8px; background: #1a3a8a; color: #fff; }
                  img { max-width: 100%; border-radius: 8px; }
                </style></head>
                <body>__BODY__</body></html>
                """.replace("__TITLE__", esc(title)).replace("__BODY__", body);
    }

    private static ResponseEntity<String> html(HttpStatus status, String content) {
        return ResponseEntity.status(status).contentType(MediaType.TEXT_HTML).body(content);
    }

    /** Opened on the phone/tablet to take the pass photo. */
    @GetMapping("/photo/{code}")
    public ResponseEntity<String> capturePage(@PathVariable String code) {
        Optional<Pass> found = passes.getPass(code);
        if (found.isEmpty()) {
            return html(HttpStatus.NOT_FOUND, page("Not found", "<h2>Pass not found</h2>"));
        }
        Pass pass = found.get();
        boolean existing = photos.hasPhoto(pass.code());

        String body = """
                <h2>Pass photo</h2>
                <p><strong>__NAME__</strong><br>__TYPE__ pass<br>__CODE__</p>
                <img id="preview" __PREVIEW__>
                <label class="btn">__LABEL__
                  <input id="file" type="file" accept="image/*" capture="user" hidden>
                </label>
                <button id="save" class="btn" hidden>Save photo</button>
                <p id="msg"></p>
                <script>
                  var CODE = "__CODE__";
                  var blob = null;
                  var fileInput = document.getElementById('file');
                  var preview = document.getElementById('preview');
                  var saveBtn = document.getElementById('save');
                  var msg = document.getElementById('msg');

                  fileInput.addEventListener('change', function () {
                    var file = fileInput.files[0];
                    if (!file) return;
                    var img = new Image();
                    img.onload = function () {
                      // Shrink to 800px so uploads are fast and small
                      var scale = Math.min(1, 800 / Math.max(img.width, img.height));
                      var canvas = document.createElement('canvas');
                      canvas.width = Math.round(img.width * scale);
                      canvas.height = Math.round(img.height * scale);
                      canvas.getContext('2d').drawImage(img, 0, 0, canvas.width, canvas.height);
                      canvas.toBlob(function (b) {
                        blob = b;
                        preview.src = URL.createObjectURL(b);
                        preview.hidden = false;
                        saveBtn.hidden = false;
                        msg.textContent = 'Happy with it? Tap Save photo.';
                      }, 'image/jpeg', 0.85);
                    };
                    img.src = URL.createObjectURL(file);
                  });

                  saveBtn.addEventListener('click', function () {
                    saveBtn.disabled = true;
                    msg.textContent = 'Saving...';
                    fetch('/api/photo/' + CODE, {
                      method: 'POST',
                      headers: { 'Content-Type': 'image/jpeg' },
                      body: blob
                    })
                      .then(function (r) { return r.json(); })
                      .then(function (d) {
                        msg.textContent = d.ok ? 'Photo saved!' : 'Could not save: ' + d.reason;
                        saveBtn.disabled = false;
                      })
                      .catch(function () {
                        msg.textContent = 'Network error. Try again.';
                        saveBtn.disabled = false;
                      });
                  });
                </script>
                """
                .replace("__NAME__", esc(pass.name()))
                .replace("__TYPE__", esc(pass.type().label().toUpperCase()))
                .replace("__CODE__", esc(pass.code()))
                .replace("__PREVIEW__", existing ? "src=\"/api/photo/" + esc(pass.code()) + "\"" : "hidden")
                .replace("__LABEL__", existing ? "Retake photo" : "Take photo");

        return html(HttpStatus.OK, page("Pass photo", body));
    }

    /** For gate staff: shows who the pass belongs to and their photo. */
    @GetMapping("/view/{code}")
    public ResponseEntity<String> viewPage(@PathVariable String code) {
        Optional<Pass> found = passes.getPass(code);
        if (found.isEmpty()) {
            return html(HttpStatus.NOT_FOUND, page("Not found", "<h2>Pass not found</h2>"));
        }
        Pass pass = found.get();

        String picture = photos.hasPhoto(pass.code())
                ? "<img src=\"/api/photo/" + esc(pass.code()) + "\">"
                : "<p>No photo on file.</p>";

        String body = "<h2>" + esc(pass.name()) + "</h2>"
                + "<p>" + esc(pass.type().label().toUpperCase()) + " pass<br>" + esc(pass.code()) + "</p>"
                + picture;
        return html(HttpStatus.OK, page("Pass", body));
    }

    /** Receives the photo (raw image bytes in the request body). */
    @PostMapping(value = "/api/photo/{code}", consumes = "image/*")
    public ResponseEntity<PhotoService.SaveResult> upload(
            @PathVariable String code,
            @RequestBody byte[] image,
            @RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType) {
        PhotoService.SaveResult result = photos.setPhoto(code, image, contentType);
        return ResponseEntity.status(result.ok() ? HttpStatus.OK : HttpStatus.BAD_REQUEST).body(result);
    }

    /** Sends the decrypted photo back. */
    @GetMapping("/api/photo/{code}")
    public ResponseEntity<byte[]> download(@PathVariable String code) {
        Optional<PhotoService.PhotoData> photo = photos.getPhoto(code);
        if (photo.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.get().mimeType()))
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(photo.get().image());
    }
}
