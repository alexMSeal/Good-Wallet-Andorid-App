# 💰 Good-Wallet

Good-Wallet is a modern Android expense tracker application designed to help users manage personal finances in a clean, simple, and structured way. It allows users to track income and expenses, organize transactions into multiple wallets (albums), and visualize financial data through interactive dashboards.

---

## 🎯 Features

### 🏠 Home Screen
- View all transactions in a clean, chronological list
- Add, edit, and delete transactions
- Balance card showing total income, expense, and net balance
- Monthly filtering and navigation
- Category-based transaction display

### 📊 Dashboard
- Visual representation of financial data
- Income vs Expense charts
- Category breakdown (pie/chart style analysis)
- Monthly financial trends
- Quick insights into spending habits

### 💼 Wallet / Album Management
- Create multiple wallets (albums) for different purposes
- Switch between wallets easily
- Edit or delete existing wallets
- Trash bin support for deleted albums
- Default wallet protection (cannot be deleted)

### ⚙️ Settings
- Edit username and profile picture
- Change currency settings (symbol and placement)
- Manage deleted albums via Trash Bin
- Restore or permanently delete removed data

### ✏️ Edit Transaction Screen
- Add and edit transaction details
- Select category (income/expense types)
- Input amount and notes
- Attach receipt image (if enabled)
- Set transaction date

---

## 🛠️ Tech Stack

- Kotlin
- Android Jetpack Compose
- MVVM Architecture
- ViewModel + StateFlow
- DataStore (for persistent settings)
- Android Studio

---

## 📁 Project Structure

- **HomeScreen** – Transaction list, balance overview, CRUD operations
- **DashboardScreen** – Charts and financial analytics
- **WalletScreen** – Manage multiple wallets/albums
- **SettingsScreen** – User preferences and trash bin management
- **EditTransactionScreen** – Add/edit transaction details

---

## 🚀 Getting Started

### 1. Clone the repository
```bash
git clone https://github.com/your-username/good-wallet.git
```

### 2. Open in Android Studio
- Open Android Studio
- Click "Open"
- Select the cloned project folder

### 3. Run the app
- Let Gradle sync finish
- Click ▶ Run on emulator or physical device

👨‍💻 Author
Developed by Alex

Thanks to the Android developer community and Jetpack Compose documentation, teachers, and friends for guidance and inspiration.
