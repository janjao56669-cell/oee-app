# OEE App (Android)

แอป Android ที่เปิดเว็บ https://oee-web.sasatornua.workers.dev/ แบบเต็มจอ
แก้เว็บเมื่อไร แอปเห็นการเปลี่ยนแปลงทันที ไม่ต้องติดตั้งใหม่

## ดาวน์โหลด APK
ไปที่แท็บ **Releases** ของ repo นี้ แล้วกดไฟล์ `OEE-v1.0.x.apk` ของเวอร์ชันล่าสุด

## สิ่งที่แอปเพิ่มจากเว็บ (ไม่ได้แก้เว็บ)
- ปุ่มส่งออก Excel / ZIP: บันทึกไฟล์ลงโฟลเดอร์ Downloads แล้วเปิดไฟล์ให้
- ปุ่มแนบรูป: เลือกถ่ายรูปด้วยกล้อง หรือเลือกจากคลังรูปได้
- ไม่มีอินเทอร์เน็ต: แสดงหน้า "เชื่อมต่อระบบไม่ได้" พร้อมปุ่มลองใหม่
- ปุ่มย้อนกลับของมือถือ: ย้อนหน้าในแอป

## การสร้าง APK
ทุกครั้งที่ push เข้า `main` GitHub Actions จะสร้าง APK ให้อัตโนมัติ (ไฟล์ `.github/workflows/build-apk.yml`)
หรือกดสร้างเองได้ที่แท็บ Actions → Build APK → Run workflow

### ลายเซ็นถาวร (แนะนำ)
ถ้ายังไม่ตั้ง Secrets ระบบจะใช้ลายเซ็นชั่วคราว ซึ่งเปลี่ยนทุกครั้งที่สร้าง
ทำให้ติดตั้งเวอร์ชันใหม่ทับไม่ได้ ต้องถอนแอปเก่าก่อน
ตั้ง Secrets 4 ตัวนี้ใน Settings → Secrets and variables → Actions เพื่อให้ติดตั้งทับได้:
`OEE_KEYSTORE_BASE64`, `OEE_KEYSTORE_PASSWORD`, `OEE_KEY_ALIAS`, `OEE_KEY_PASSWORD`

## โครงสร้าง
- `capacitor.config.json` – URL ของเว็บที่แอปเปิด
- `www/error.html` – หน้าแสดงตอนไม่มีอินเทอร์เน็ต
- `android/app/src/main/java/com/tsgp/oee/MainActivity.java` – ปุ่มแนบรูป (กล้อง + คลังรูป)
- `android/app/src/main/java/com/tsgp/oee/DownloadPlugin.java` – บันทึกไฟล์ที่เว็บส่งออก
- `tools/make_icons.py` – สร้างไอคอนแอป
