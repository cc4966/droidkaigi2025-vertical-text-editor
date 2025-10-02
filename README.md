# Vertical Text Editor
Android向け縦書きテキストエディタのベースとなる基礎技術を実装したサンプルアプリです。
DroidKaigi 2025の「[Android 16 × Jetpack Composeで縦書きテキストエディタを作ろう](https://2025.droidkaigi.jp/timetable/946512/)」のために作られました。
発表資料は https://speakerdeck.com/cc4966/vertical-text-editor-with-compose-on-android-16 にあります。

## 概要
Android上で縦書きテキスト表示・編集を実現するための技術を検証・実装したものです。
上部のドロップダウンで画面を切り替えられます。

## 主な検証技術
### 1. 縦書き表示
- `Paint.VERTICAL_TEXT_FLAG`を使用した縦書き表示
- `VerticalTextLayout`を使用した縦書き表示

### 2. テキスト編集
- PlatformTextInputModifierNodeを利用したCustomEditor
- LocalTextInputServiceを利用したCustomTextInput
- TextFieldStateとInterceptPlatformTextInputを利用したInterceptEditor
