# Pager Gesture Checklist — Why swipe/scroll fails in Compose (master)

Use this top-to-bottom; if even one item is wrong, swipe will fail.

1️⃣ WRONG PAGER IMPORT (MOST COMMON SILENT KILLER)

❌ WRONG
```kotlin
import com.google.accompanist.pager.VerticalPager
// or old accompanist artifacts still in Gradle
```

✅ CORRECT
```kotlin
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
```

🔍 VERIFY
- `build.gradle` has NO accompanist pager
- Only `androidx.compose.foundation` pager is used

2️⃣ PAGER INSIDE A SCROLLABLE PARENT (100% BREAKS SWIPE)

❌ WRONG
```kotlin
LazyColumn {
    item {
        VerticalPager { ... } // DEAD
    }
}

Column(
    Modifier.verticalScroll(...)
) {
    VerticalPager { ... } // DEAD
}
```

✅ CORRECT
Pager must be the ONLY scrollable parent.
```kotlin
Box(Modifier.fillMaxSize()) {
    VerticalPager(...)
}
```

3️⃣ ANY .clickable {} ABOVE OR AROUND PAGER

❌ WRONG
```kotlin
Box(
    Modifier
        .fillMaxSize()
        .clickable { /* toggle UI */ }
) {
    VerticalPager { ... }
}
```

✅ CORRECT
- Remove `.clickable` from wrappers
- Handle taps inside page content

4️⃣ ANY .pointerInput {} THAT DOES NOT FORWARD EVENTS

❌ WRONG
```kotlin
.pointerInput(Unit) {
    detectTapGestures { /* consumes */ }
}

.pointerInput(Unit) { } // STILL consumes
```

✅ CORRECT
- Remove entirely or
- Use it only on content, not pager wrapper
- Never attach empty `pointerInput` on the pager's wrapper

5️⃣ ZOOM / TRANSFORMABLE EATS SWIPE (CRITICAL)

❌ WRONG
```kotlin
Modifier.transformable(state)
// When scale == 1f → still eats drag → pager never moves
```

✅ CORRECT (PRODUCTION PATTERN)
```kotlin
if (scale > 1f) {
    Modifier.transformable(state)
} else {
    Modifier // allow pager swipe
}
```

6️⃣ graphicsLayer WITH TRANSLATION ON FULL SCREEN

❌ WRONG
```kotlin
Modifier
    .fillMaxSize()
    .graphicsLayer {
        translationX = offset.x
        translationY = offset.y
    }
```

✅ CORRECT
- Apply `graphicsLayer` only to the Image, not container.

7️⃣ FULL-SCREEN OVERLAY ABOVE PAGER

❌ WRONG
```kotlin
Box(Modifier.matchParentSize().background(...))
// Even without clickable → blocks input.
```

✅ CORRECT
```kotlin
Box(
    Modifier
        .matchParentSize()
        .clearAndSetSemantics { } // OPTIONAL
)
// OR only overlay controls, not full screen.
```

8️⃣ Pager Height IS NOT fillMaxSize()

❌ WRONG
```kotlin
VerticalPager(
    modifier = Modifier.wrapContentHeight()
)
```

✅ CORRECT
```kotlin
VerticalPager(
    modifier = Modifier.fillMaxSize()
)
```

9️⃣ PAGE CONTENT USES fillMaxHeight() INSIDE PAGER

❌ WRONG
```kotlin
VerticalPager {
    Column(Modifier.fillMaxHeight()) { ... }
}
```

✅ CORRECT
```kotlin
Box(Modifier.fillMaxSize())
```

🔟 PAGE COUNT IS WRONG / STATIC

❌ WRONG
```kotlin
rememberPagerState { 1 } // always 1 page
```

✅ CORRECT
```kotlin
rememberPagerState { pageCount }
```
Confirm:
```kotlin
pageCount > 1
```

1️⃣1️⃣ PDF RENDER BLOCKING UI THREAD (CAUSES “SWIPE DOES NOTHING”)

❌ WRONG
```kotlin
page.render(bitmap, null, null, ...)
// on Main thread → UI freezes → swipe ignored
```

✅ CORRECT
```kotlin
withContext(Dispatchers.Default) {
    page.render(...)
}
```

1️⃣2️⃣ PDF BITMAP TOO LARGE (OOM / BLACK SCREEN / DELAY)

❌ WRONG
```kotlin
Bitmap.createBitmap(page.width, page.height, ...)
```

✅ CORRECT
```kotlin
val screenWidth = displayMetrics.widthPixels
val scale = screenWidth / page.width.toFloat()
val targetHeight = (page.height * scale).toInt()

Bitmap.createBitmap(screenWidth, targetHeight, ...)
```

1️⃣3️⃣ PdfRenderer Page NOT CLOSED

❌ WRONG
```kotlin
val page = renderer.openPage(index)
// (no close)
```

✅ CORRECT
```kotlin
DisposableEffect(pageIndex) {
    onDispose { page.close() }
}
```

1️⃣4️⃣ Overlay Animations Rebuilding Entire Pager

❌ WRONG
```kotlin
AnimatedContent(targetState = showOverlay) {
    VerticalPager(...)
}
```

✅ CORRECT
- Overlay outside pager, pager never rebuilt.

1️⃣5️⃣ Nested Box(matchParentSize()) Inside Pager Page

This silently breaks gesture dispatch.

✅ RULE
Inside pager page:
- ONE root container
- No nested matchParentSize()

1️⃣6️⃣ ZIP / IMAGE VIEWER SHARE SAME BUG PATTERN

Everything above applies equally to:
- ImageViewer
- ZipViewer (file list scrolling)
- PDF Viewer
Fix once → reuse pattern.

---

## LAST CULPRITS — Final checks if swipes still fail (do this top-to-bottom)
Even after the six phases, these four issues are the last real culprits that silently break paging. Check them in this order.

1️⃣ Pager orientation mismatch (VERY common)

- Symptom: Swipes seem to do nothing or feel inverted on some devices.
- Check:
```kotlin
VerticalPager(
    state = pagerState,
    orientation = Orientation.Vertical // MUST be explicit
)
// For images:
HorizontalPager(
    state = pagerState,
    orientation = Orientation.Horizontal
)
```
- Note: If orientation is implicit, Compose may infer incorrectly when nested — set explicitly.

2️⃣ Nested scroll parent above pager

- Symptom: Pager never receives scroll events; scrolls are captured by a parent.
- Search for these usages above the viewer: `.verticalScroll`, `.nestedScroll`, `LazyColumn`, `ScrollableColumn`.
- Fix: Ensure the pager is not inside any scrollable parent — it must be the only scrollable container.

3️⃣ Zoom state never returning to 1f

- Symptom: One page remains zoomed (scale > 1f) and pager is effectively blocked.
- Verify:
  - Double-tap resets scale to exactly `1f`.
  - Rotation reset does not leave `scale > 1f`.
  - New page starts with `scale = 1f` (use `remember { ZoomState() }` per page).
- Fix: Ensure double-tap logic and page lifecycle always reset scale/offset/rotation on close or page change.

4️⃣ PagerState recreated unintentionally

- Symptom: Pager loses state or swipe behavior breaks intermittently.
- Correct usage:
```kotlin
val pagerState = rememberPagerState(
    initialPage = startPage,
    pageCount = { pageCount }
)
```
- DO NOT create `pagerState` inside `AnimatedVisibility`, `AnimatedContent`, or under `if (overlayVisible)` that causes recomposition-driven recreation.
- If `pagerState` is recreated, swipe behavior can fail randomly — keep it stable and remembered at the screen scope.

---

Run these four checks as the final gate if swipes still fail — they catch the last silent failures that are hard to observe during development.
