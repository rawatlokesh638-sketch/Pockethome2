# 📱 Pockethome - Smart Indian Home Expense Tracker & AI Money Growth Advisor

Ghar ka automated hisaab-kitab, real-time SMS expense tracker aur Institutional AI Wealth Advisor.

---

## 🌐 GitHub Pages Website Setup Guide (Website Live Kaise Karein)

Aapki website files (`docs/`, `public/`, `index.html`, `pockethome.apk`) aur GitHub Actions workflows (`.github/workflows/deploy-pages.yml` aur `build-apk.yml`) bilkul tayyar hain!

### Step 1: Push code to your GitHub Repository
Agar aapne AI Studio ke **Export / Push to GitHub** option se push kiya hai, toh aapke GitHub repo mein sabhi files aa chuki hain.
Terminal se push karne ke liye:
```bash
git remote add origin https://github.com/<YOUR-USERNAME>/<YOUR-REPO-NAME>.git
git branch -M main
git push -u origin main
```

---

### Step 2: 1-Click mein GitHub Website On Karein (Only 30 Seconds!)
1. Apne GitHub repository ko browser mein kholein (`https://github.com/<username>/<repo>`).
2. Upar **Settings** tab par click karein.
3. Left sidebar mein **Pages** par click karein.
4. **Build and deployment** section ke andar:
   - **Source**: **Deploy from a branch** select karein.
   - **Branch**: `main` select karein aur folder dropdown mein `/docs` ya `/ (root)` select karein.
   - **Save** button dabayein!
   *(Alternative: Source mein **GitHub Actions** bhi select kar sakte hain, `.github/workflows/deploy-pages.yml` automatically deploy kar dega!)*
5. 1 minute mein aapki website live ho jayegi:
   👉 **`https://<YOUR-USERNAME>.github.io/<YOUR-REPO-NAME>/`**

---

## 📲 Website Features
- **Header me Install Button (APK)**: Website ke sabse upar header me **"📲 Install App"** button hai, jisko click karte hi `Pockethome.apk` phone me direct download ho jata hai (bina zip ke direct installable APK).
- **SMS Transaction Live Sync**: Android app se real-time sync + website par SMS paste karke instant analysis.
- **Financial Dashboard**: Income, Expenses, Savings, Balance, Category breakdown, Trend graphs.
- **AI Wealth Advisor**: Wealth health score, kitna grow hoga, kaha se bachayein, SIP simulator.
- **Pockethome PRO Modal**: ₹15/month ya ₹120/year ka transparent subscription system with UPI payment verification.

---

## ⭐ PRO Plan Features (Strictly Gated - Active Hone Par Hee Milega)

User requirement ke mutabik:
- **Koi bhi feature app me chhipa hua nahi hai (100% visible)**. Sabhi screens par features visible hain taaki user ko pata chale ki kya kya milta hai.
- **Paid plan ke features plan buy / activate karne ke baad hee milte hain**:
  1. **Daily AI Institutional Wealth Advisor & Simulator**: Free users ko preview audit dikhta hai; custom risk profile (Conservative, Moderate, Aggressive), custom monthly SIP slider, inflation toggle aur multi-asset portfolio plan PRO active hone par unlock hota hai.
  2. **Export to PDF & Excel Spreadsheet**: Reports screen par visible hai with `PRO ⭐ (₹15/mo)` badge; export click karne par PRO plan purchase requirement aati hai; active hone par 1-click clean export chalta hai.
  3. **Automatic Bill Reminders**: Bills screen par toggle switch PRO plan active hone par hee on hota hai.
  4. **Category Overspending Alerts**: Budget screen par custom 80%/100% category limit alerts PRO plan me activate hoti hain.
  5. **Recurring Auto-Repeat Expense**: Add expense screen par auto-repeat switch PRO active hone par activate hota hai.
  6. **Bill Photo / Receipt Cloud Vault**: Photo attachment PRO plan me cloud store hota hai.
  7. **Real-Time Continuous SMS Sync**: Bank & UPI SMS background sync.
