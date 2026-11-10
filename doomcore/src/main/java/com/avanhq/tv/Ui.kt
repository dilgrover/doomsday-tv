package com.avanhq.tv

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Small helpers for building the screens in code. Every screen is laid out on a 1920x1080
 * stage in the design's own pixels (the Chromecast draws apps at 1080p), so positions and
 * sizes match the design artifact one to one.
 */
object Ui {
    const val W = 1920
    const val H = 1080

    /** Avan's state for every orb on screen: idle, listening, thinking, acting, speaking, approval. */
    @JvmStatic var orbState = "idle"

    /** Screenshots only (ShotsTest): draw one frame of every animation and stop. */
    @JvmStatic var still = false

    // The accent colour for focus rings: six choices, Cyan by default. The Doomsday screens
    // colour every ring green while they show (ringColour).
    val ACCENTS = listOf(
        "Cyan" to Color.rgb(0x3f, 0xd0, 0xff),
        "Violet" to Color.rgb(0x8b, 0x7b, 0xff),
        "Rose" to Color.rgb(0xff, 0x5c, 0x8a),
        "Amber" to Color.rgb(0xf5, 0xa5, 0x24),
        "Green" to Color.rgb(0x3e, 0xcf, 0x8e),
        "Slate" to Color.rgb(0x9a, 0xa7, 0xb4),
    )
    var accent = ACCENTS[0].second

    fun loadAccent(ctx: Context) {
        val saved = ctx.getSharedPreferences("avan", Context.MODE_PRIVATE).getInt("accent", ACCENTS[0].second)
        accent = ACCENTS.firstOrNull { it.second == saved }?.second ?: ACCENTS[0].second
    }

    fun saveAccent(ctx: Context, color: Int) {
        accent = color
        ringColour(null)
        ctx.getSharedPreferences("avan", Context.MODE_PRIVATE).edit().putInt("accent", color).apply()
    }
    val gold = Color.rgb(240, 200, 120)
    val text = Color.WHITE
    val soft = Color.argb(200, 255, 255, 255)
    val dim = Color.argb(150, 255, 255, 255)
    val amber = Color.rgb(245, 165, 36)
    val green = Color.rgb(62, 207, 142)
    val red = Color.rgb(255, 99, 99)

    fun alpha(color: Int, a: Float): Int =
        Color.argb((a * 255).toInt().coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

    /** A rounded, pre-baked translucent panel (no live blur: too heavy for TV graphics). */
    fun panel(radius: Float = 28f, fill: Int = Color.argb(150, 12, 16, 28), border: Int = Color.argb(40, 255, 255, 255)): Drawable =
        GradientDrawable().apply {
            cornerRadius = radius
            setColor(fill)
            setStroke(2, border)
        }

    // Every focus ring made, so a screen with its own look can colour them while it shows
    // (Doomsday: Doom's green, not the accent; D, 10 Nov: "10/10, a reel worth theme").
    private val rings = java.util.WeakHashMap<GradientDrawable, Int>()
    private var ringOverride: Int? = null

    private fun ring(d: GradientDrawable, width: Int): GradientDrawable {
        d.setStroke(width, ringOverride ?: accent)
        rings[d] = width
        return d
    }

    /** Focus rings in [color] from now on (all of them, the ones already made too); null: the accent. */
    fun ringColour(color: Int?) {
        ringOverride = color
        for ((d, w) in rings.entries.toList()) d.setStroke(w, color ?: accent)
    }

    /** A focusable chip or tile: brighter with an accent ring when the remote is on it. */
    fun focusable(radius: Float, fill: Int = Color.argb(140, 16, 20, 34), focusedFill: Int = Color.argb(220, 28, 36, 56)): Drawable {
        val normal = panel(radius, fill, Color.argb(36, 255, 255, 255))
        val focused = ring(GradientDrawable().apply {
            cornerRadius = radius
            setColor(focusedFill)
        }, 4)
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), focused)
            addState(intArrayOf(), normal)
        }
    }

    /** Only a ring, shown when focused: drawn over an app's banner. */
    fun focusRing(radius: Float): Drawable = StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_focused), ring(GradientDrawable().apply {
            cornerRadius = radius
            setColor(Color.TRANSPARENT)
        }, 5))
        addState(intArrayOf(), ColorDrawable(Color.TRANSPARENT))
    }

    fun text(
        ctx: Context,
        value: CharSequence = "",
        size: Float = 26f,
        color: Int = text,
        bold: Boolean = false,
        lines: Int = 1,
        gravity: Int = Gravity.START,
        spacing: Float = 0f,
    ): TextView = TextView(ctx).apply {
        text = value
        setTextSize(TypedValue.COMPLEX_UNIT_PX, readable(size))
        setTextColor(color)
        // Semibold for emphasis, as the design.
        typeface = Fonts.of(ctx, if (bold) Fonts.SEMIBOLD else Fonts.REGULAR)
        includeFontPadding = false
        maxLines = lines
        if (lines == 1) {
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
        } else {
            ellipsize = TextUtils.TruncateAt.END
        }
        this.gravity = gravity
        if (spacing != 0f) letterSpacing = spacing
        // A faint shadow only: a wide dark one fringed every letter on a Google TV and made it look bold.
        setShadowLayer(3f, 0f, 1f, Color.argb(80, 0, 0, 0))
    }

    /**
     * One set of text sizes for every screen.
     * Anything under 40 px on the 1920 stage snaps to one of five steps: 20 (labels, small
     * print), 22 (second lines), 24 (body and buttons), 28 (card titles), 32 (big lines). Each
     * is a little larger than the design's own, for reading from the sofa.
     */
    fun readable(size: Float): Float = when {
        size >= 40f -> size
        size < 19f -> 20f
        size < 22f -> 22f
        size < 26f -> 24f
        size < 30f -> 28f
        size < 36f -> 32f
        else -> 36f
    }

    /** "●  words" with the dot in its own colour. */
    fun dot(color: Int, words: String): CharSequence = android.text.SpannableString("●  $words").apply {
        setSpan(android.text.style.ForegroundColorSpan(color), 0, 1, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    /** A small spaced-out heading, like "TODAY" or "HOME NOW". */
    fun label(ctx: Context, value: String, color: Int = dim) = text(ctx, value.uppercase(), 18f, color, spacing = 0.16f).apply {
        typeface = Fonts.of(ctx, Fonts.BOLD)
    }

    /** A pill button. */
    /** Shrink a one-line text to fit its width, from [max] down to [min], before it's cut off. */
    fun fit(v: TextView, min: Float, max: Float): TextView = v.apply {
        gravity = Gravity.CENTER_VERTICAL or (gravity and Gravity.HORIZONTAL_GRAVITY_MASK)
        setAutoSizeTextTypeUniformWithConfiguration(readable(min).toInt(), readable(max).toInt(), 1, TypedValue.COMPLEX_UNIT_PX)
    }

    fun chip(ctx: Context, value: String, size: Float = 26f, onClick: () -> Unit): TextView =
        text(ctx, value, size, text, bold = true, gravity = Gravity.CENTER).apply {
            background = focusable(40f)
            setPadding(30, 0, 30, 0)
            isFocusable = true
            isClickable = true
            setOnClickListener { onClick() }
            focusMotion(this)
        }

    /** The design's easing for focus (`cubic-bezier(.2,.8,.2,1)`). */
    val EASE = android.view.animation.PathInterpolator(0.2f, 0.8f, 0.2f, 1f)

    /**
     * The design's focus on anything you can pick (HomeV4 `.f` and `.sug`): it grows a little
     * over 0.45 s and a light sweeps across it once.
     */
    fun focusMotion(v: View, grow: Float = 1.06f) {
        val sheen = Sheen()
        v.foreground = sheen
        // The light sweep stays inside the rounded shape, and a grown chip isn't cut by the row
        // it sits in.
        v.clipToOutline = true
        v.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                var p = view.parent
                repeat(3) {
                    (p as? android.view.ViewGroup)?.let { g -> g.clipChildren = false; g.clipToPadding = false }
                    p = p?.parent
                }
            }

            override fun onViewDetachedFromWindow(view: View) {}
        })
        v.setOnFocusChangeListener { view, has ->
            view.animate().scaleX(if (has) grow else 1f).scaleY(if (has) grow else 1f).setDuration(if (still) 0 else 450).setInterpolator(EASE).start()
            if (has && !still) sheen.sweep()
        }
    }

    /** A soft white band that crosses its view once, slanted like the design's `sheen`. */
    class Sheen : Drawable() {
        private var at = -1f
        private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        private val clip = android.graphics.Path()
        private var anim: android.animation.ValueAnimator? = null

        fun sweep() {
            anim?.cancel()
            anim = android.animation.ValueAnimator.ofFloat(-0.4f, 1.4f).apply {
                duration = 1100 // brighter and a little slower: 0.9 s was too quick to see
                interpolator = android.view.animation.AccelerateDecelerateInterpolator()
                addUpdateListener { at = it.animatedValue as Float; invalidateSelf() }
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        at = -1f
                        invalidateSelf()
                    }
                })
                start()
            }
        }

        override fun draw(c: android.graphics.Canvas) {
            if (at < -0.39f || at > 1.39f) return
            val b = bounds
            val w = b.width().toFloat()
            val h = b.height().toFloat()
            val band = w * 0.36f
            val x = b.left + at * w
            paint.shader = android.graphics.LinearGradient(x - band / 2, 0f, x + band / 2, 0f, intArrayOf(Color.TRANSPARENT, Color.argb(95, 255, 255, 255), Color.TRANSPARENT), null, android.graphics.Shader.TileMode.CLAMP)
            clip.reset()
            clip.addRoundRect(b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.bottom.toFloat(), h / 2, h / 2, android.graphics.Path.Direction.CW)
            c.save()
            c.clipPath(clip)
            c.skew(-0.32f, 0f)
            c.translate(h * 0.32f, 0f)
            c.drawRect(b.left - w, b.top.toFloat(), b.right + w, b.bottom.toFloat(), paint)
            c.restore()
        }

        override fun setAlpha(alpha: Int) = Unit
        override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) = Unit
        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
    }

    fun at(x: Int, y: Int, w: Int = ViewGroup.LayoutParams.WRAP_CONTENT, h: Int = ViewGroup.LayoutParams.WRAP_CONTENT) =
        FrameLayout.LayoutParams(w, h).apply {
            leftMargin = x
            topMargin = y
        }

    /** Placed against the stage's right edge (x is the right margin). */
    fun atRight(right: Int, y: Int, w: Int = ViewGroup.LayoutParams.WRAP_CONTENT, h: Int = ViewGroup.LayoutParams.WRAP_CONTENT) =
        FrameLayout.LayoutParams(w, h, Gravity.TOP or Gravity.END).apply {
            rightMargin = right
            topMargin = y
        }

    fun column(ctx: Context, gap: Int = 0): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        if (gap > 0) {
            showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
            dividerDrawable = spacer(gap)
        }
    }

    fun row(ctx: Context, gap: Int = 0): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        if (gap > 0) {
            showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
            dividerDrawable = spacer(gap)
        }
    }

    private fun spacer(size: Int): Drawable = object : ColorDrawable(Color.TRANSPARENT) {
        override fun getIntrinsicWidth() = size
        override fun getIntrinsicHeight() = size
    }

    /** A round icon badge with one character or symbol in it. */
    /**
     * One icon set everywhere: Google's
     * Material Icons (Apache 2.0, assets/fonts, from the material-design-icons-iconfont 6.7.0
     * package). The old symbols are mapped, so "☾" or "🚘" from Avan still draw as icons.
     */
    private val ICONS = mapOf(
        "▦" to '\uE878', "event" to '\uE878',
        "delivery" to '\uEA72', // delivery_dining
        "☾" to '\uEA46', "prayer" to '\uEA46',
        "Ⓤ" to '\uE533', "Ⓣ" to '\uE533', "tube" to '\uE533',
        "☆" to '\uEA27', "cricket" to '\uEA27',
        "☀" to '\uE0F0', "💡" to '\uE0F0', "lights" to '\uE0F0',
        "🌡" to '\uF076', "temperature" to '\uF076',
        "🚘" to '\uE531', "car" to '\uE531',
        "⛨" to '\uE9E0', "security" to '\uE9E0',
        "ϟ" to '\uEA0B', "energy" to '\uEA0B',
        "⇣" to '\uE63E', "internet" to '\uE63E',
        "🔔" to '\uE7F4', "!" to '\uE7F4', "alerts" to '\uE7F4',
        "🎬" to '\uE02C', "movie" to '\uE02C',
        "⏮" to '\uE045', "⏭" to '\uE044', "⤮" to '\uE043', "↻" to '\uE040',
        "🔈" to '\uE050', "♫" to '\uE405', "music" to '\uE405',
        "✦" to '\uE65F', "✧" to '\uE65F', "avan" to '\uE65F',
        "❚❚" to '\uE034', "▶" to '\uE037',
        "❄" to '\uEB3B', "♨" to '\uE80E', "〜" to '\uE176',
    )
    private var iconFace: Typeface? = null

    /** A Material icon for a known symbol or name; any other text as it is. */
    fun icon(ctx: Context, symbol: String, size: Float, tint: Int): TextView {
        val glyph = ICONS[symbol]
        return text(ctx, glyph?.toString() ?: symbol, size, tint, gravity = Gravity.CENTER).apply {
            if (glyph != null) {
                val face = iconFace ?: runCatching { Typeface.createFromAsset(ctx.assets, "fonts/MaterialIcons-Regular.ttf") }.getOrNull()?.also { iconFace = it }
                if (face != null) {
                    typeface = face
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, size * 1.25f) // icons read small at text size
                    setShadowLayer(0f, 0f, 0f, 0)
                }
            }
        }
    }

    /** A Material Icons glyph by its code point (e.g. "\uE405", music_note). */
    fun glyph(ctx: Context, code: String, size: Float, tint: Int): TextView =
        text(ctx, code, size, tint, gravity = Gravity.CENTER).apply {
            val face = iconFace ?: runCatching { Typeface.createFromAsset(ctx.assets, "fonts/MaterialIcons-Regular.ttf") }.getOrNull()?.also { iconFace = it }
            if (face != null) {
                typeface = face
                setTextSize(TypedValue.COMPLEX_UNIT_PX, size * 1.25f)
            }
        }

    fun badge(ctx: Context, symbol: String, tint: Int): TextView =
        icon(ctx, symbol, 26f, tint).apply {
            background = GradientDrawable().apply {
                cornerRadius = 16f
                setColor(alpha(tint, 0.16f))
            }
            layoutParams = LinearLayout.LayoutParams(68, 68)
        }

    /** One row of a card: a badge, a title and a line under it. */
    fun infoRow(ctx: Context, symbol: String, tint: Int, title: String, sub: String?, subColor: Int = dim): LinearLayout =
        row(ctx, 22).apply {
            addView(badge(ctx, symbol, tint))
            val words = column(ctx, 6)
            words.addView(text(ctx, title, 24f, Ui.text, bold = true))
            if (!sub.isNullOrBlank()) words.addView(text(ctx, sub, 20f, subColor))
            addView(words, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }

    fun gone(v: View, hide: Boolean) {
        v.visibility = if (hide) View.GONE else View.VISIBLE
    }
}

/**
 * Manrope, the design's typeface, bundled in
 * assets/fonts under the SIL Open Font Licence (Manrope-OFL.txt). Android's default font made
 * the TV look unlike the design. A missing glyph (✦) falls back to the system font.
 */
object Fonts {
    const val LIGHT = 300
    const val REGULAR = 500
    const val SEMIBOLD = 600
    const val BOLD = 700
    private val cache = HashMap<Int, Typeface>()

    fun of(ctx: android.content.Context, weight: Int): Typeface = cache.getOrPut(weight) {
        runCatching { Typeface.createFromAsset(ctx.assets, "fonts/manrope_$weight.ttf") }
            .getOrElse { Typeface.create("sans-serif", if (weight >= SEMIBOLD) Typeface.BOLD else Typeface.NORMAL) }
    }
}
