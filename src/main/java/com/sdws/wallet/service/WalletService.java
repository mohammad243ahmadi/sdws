package com.sdws.wallet.service;

import com.sdws.wallet.model.*;
import com.sdws.wallet.model.Enums.*;
import com.sdws.wallet.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final UserRepository users;
    private final WalletRepository wallets;
    private final TransactionRepository txs;
    private final MoneyRequestRepository requests;
    private final MobileTopupRepository topups;
    private final BillPaymentRepository bills;
    private final PasswordEncoder encoder;

    // ---------- helpers ----------
    public AppUser current(String phone) {
        AppUser u = users.findByPhone(phone).orElseThrow(() -> new AccessDeniedException("Unknown user"));
        if (!u.isActive()) throw new AccessDeniedException("Account is deactivated");
        return u;
    }

    public Wallet wallet(AppUser u) {
        return wallets.findByUserId(u.getId()).orElseThrow();
    }

    private Wallet lock(AppUser u) {
        return wallets.findByUserIdForUpdate(u.getId()).orElseThrow();
    }

    private BigDecimal amount(BigDecimal a) {
        if (a == null || a.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Please enter a valid amount greater than zero.");
        return a.setScale(2, RoundingMode.HALF_UP);
    }

    private void log(AppUser u, TxType type, BigDecimal amt, String desc) {
        WalletTransaction t = new WalletTransaction();
        t.setUser(u); t.setType(type); t.setAmount(amt); t.setDescription(desc);
        txs.save(t);
    }

    private void debit(AppUser u, BigDecimal amt) {
        Wallet w = lock(u);
        if (w.getBalance().compareTo(amt) < 0)
            throw new IllegalArgumentException("Insufficient balance.");
        w.setBalance(w.getBalance().subtract(amt));
    }

    private AppUser userByPhone(String phone) {
        AppUser u = users.findByPhone(phone == null ? "" : phone.trim())
                .orElseThrow(() -> new IllegalArgumentException("No registered user with that phone number."));
        if (!u.isActive()) throw new IllegalArgumentException("That account is not active.");
        return u;
    }

    // ---------- registration / profile ----------
    @Transactional
    public void register(String fullName, String phone, String email, String password) {
        if (fullName == null || fullName.isBlank()) throw new IllegalArgumentException("Full name is required.");
        if (phone == null || !phone.matches("\\d{9,15}")) throw new IllegalArgumentException("Phone must be 9–15 digits.");
        if (password == null || password.length() < 8) throw new IllegalArgumentException("Password must be at least 8 characters.");
        if (users.existsByPhone(phone)) throw new IllegalArgumentException("This phone number is already registered.");

        AppUser u = new AppUser();
        u.setFullName(fullName.trim()); u.setPhone(phone); u.setEmail(email);
        u.setPasswordHash(encoder.encode(password));
        users.save(u);

        Wallet w = new Wallet();
        w.setUser(u);
        w.setAccountNumber(String.format("SW%010d", u.getId()));
        wallets.save(w);
    }

    @Transactional
    public void updateProfile(AppUser u, String fullName, String email, String currentPw, String newPw) {
        if (fullName == null || fullName.isBlank()) throw new IllegalArgumentException("Full name is required.");
        AppUser managed = users.findById(u.getId()).orElseThrow();
        managed.setFullName(fullName.trim());
        managed.setEmail(email);
        if (newPw != null && !newPw.isBlank()) {
            if (newPw.length() < 8) throw new IllegalArgumentException("New password must be at least 8 characters.");
            if (currentPw == null || !encoder.matches(currentPw, managed.getPasswordHash()))
                throw new IllegalArgumentException("Current password is incorrect.");
            managed.setPasswordHash(encoder.encode(newPw));
        }
    }

    // ---------- wallet operations ----------
    @Transactional
    public void add(AppUser u, BigDecimal amt) {
        amt = amount(amt);
        Wallet w = lock(u);
        w.setBalance(w.getBalance().add(amt));
        log(u, TxType.ADD, amt, "Added money to wallet");
    }

    @Transactional
    public void withdraw(AppUser u, BigDecimal amt) {
        amt = amount(amt);
        debit(u, amt);
        log(u, TxType.WITHDRAW, amt, "Withdrawal");
    }

    @Transactional
    public void send(AppUser sender, String phone, BigDecimal amt, String note) {
        transfer(sender, userByPhone(phone), amount(amt), note == null ? "" : note.trim());
    }

    /** Moves money between two wallets. Wallets are locked in id order to avoid deadlocks. */
    private void transfer(AppUser from, AppUser to, BigDecimal amt, String note) {
        if (from.getId().equals(to.getId())) throw new IllegalArgumentException("You cannot send money to yourself.");
        AppUser first = from.getId() < to.getId() ? from : to;
        AppUser second = first == from ? to : from;
        Wallet w1 = lock(first);
        Wallet w2 = lock(second);
        Wallet src = first == from ? w1 : w2;
        Wallet dst = first == from ? w2 : w1;

        if (src.getBalance().compareTo(amt) < 0) throw new IllegalArgumentException("Insufficient balance.");
        src.setBalance(src.getBalance().subtract(amt));
        dst.setBalance(dst.getBalance().add(amt));

        String suffix = note.isEmpty() ? "" : " — " + note;
        log(from, TxType.SEND, amt, "Sent to " + to.getFullName() + " (" + to.getPhone() + ")" + suffix);
        log(to, TxType.RECEIVE, amt, "Received from " + from.getFullName() + " (" + from.getPhone() + ")" + suffix);
    }

    // ---------- money requests ----------
    @Transactional
    public void requestMoney(AppUser requester, String phone, BigDecimal amt) {
        AppUser target = userByPhone(phone);
        if (target.getId().equals(requester.getId())) throw new IllegalArgumentException("You cannot request money from yourself.");
        MoneyRequest r = new MoneyRequest();
        r.setRequester(requester); r.setTarget(target); r.setAmount(amount(amt));
        requests.save(r);
    }

    @Transactional
    public void respond(AppUser me, Long requestId, boolean accept) {
        MoneyRequest r = requests.findById(requestId).orElseThrow(() -> new IllegalArgumentException("Request not found."));
        if (!r.getTarget().getId().equals(me.getId())) throw new AccessDeniedException("Not your request.");
        if (r.getStatus() != RequestStatus.PENDING) throw new IllegalArgumentException("This request was already handled.");
        if (accept) {
            if (!r.getRequester().isActive()) throw new IllegalArgumentException("The requester's account is not active.");
            transfer(me, r.getRequester(), r.getAmount(), "Money request #" + r.getId());
            r.setStatus(RequestStatus.ACCEPTED);
        } else {
            r.setStatus(RequestStatus.DECLINED);
        }
    }

    public List<MoneyRequest> sentRequests(AppUser u) { return requests.findByRequesterOrderByCreatedAtDesc(u); }
    public List<MoneyRequest> incomingRequests(AppUser u) { return requests.findByTargetOrderByCreatedAtDesc(u); }

    // ---------- top-up & bills (simulated) ----------
    @Transactional
    public void topup(AppUser u, String operator, String number, BigDecimal amt) {
        amt = amount(amt);
        if (number == null || !number.matches("\\d{9,15}")) throw new IllegalArgumentException("Enter a valid mobile number (digits only).");
        debit(u, amt);
        MobileTopup t = new MobileTopup();
        t.setUser(u); t.setOperator(operator); t.setPhoneNumber(number); t.setAmount(amt);
        topups.save(t);
        log(u, TxType.TOPUP, amt, "Top-up " + operator + " " + number);
    }

    @Transactional
    public void payBill(AppUser u, String category, String billNumber, BigDecimal amt) {
        amt = amount(amt);
        if (billNumber == null || billNumber.isBlank()) throw new IllegalArgumentException("Bill number is required.");
        debit(u, amt);
        BillPayment b = new BillPayment();
        b.setUser(u); b.setCategory(category); b.setBillNumber(billNumber.trim()); b.setAmount(amt);
        bills.save(b);
        log(u, TxType.BILL, amt, category + " bill #" + billNumber.trim());
    }

    // ---------- history ----------
    public List<WalletTransaction> history(AppUser u, String type) {
        return filter(txs.findByUserOrderByCreatedAtDescIdDesc(u), type, null);
    }

    public List<WalletTransaction> allTransactions(String type, String phone) {
        return filter(txs.findAllByOrderByCreatedAtDescIdDesc(), type, phone);
    }

    private List<WalletTransaction> filter(List<WalletTransaction> list, String type, String phone) {
        return list.stream()
                .filter(t -> type == null || type.isBlank() || t.getType().name().equals(type))
                .filter(t -> phone == null || phone.isBlank() || t.getUser().getPhone().contains(phone.trim()))
                .toList();
    }

    // ---------- admin ----------
    public List<AppUser> allUsers() { return users.findAll(); }

    public BigDecimal totalBalance() {
        return wallets.findAll().stream().map(Wallet::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public void toggleActive(AppUser admin, Long userId) {
        AppUser u = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found."));
        if (u.getId().equals(admin.getId())) throw new IllegalArgumentException("You cannot deactivate your own account.");
        u.setActive(!u.isActive());
    }
}
