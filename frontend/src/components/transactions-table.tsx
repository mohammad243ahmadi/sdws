import { ArrowDownLeft, ArrowUpRight } from "lucide-react";
import { dateTime, money, type Transaction } from "@/lib/api";

type TransactionsTableProps = { transactions: Transaction[]; showUser?: boolean };

export function TransactionsTable({ transactions, showUser = false }: TransactionsTableProps) {
  return (
    <div className="table-wrap">
      <table>
        <thead><tr>{showUser && <th>User</th>}<th>Activity</th><th>Amount</th><th>Date</th><th>Status</th></tr></thead>
        <tbody>
          {transactions.map((transaction) => {
            const credit = transaction.type === "RECEIVE" || transaction.type === "ADD";
            return <tr key={transaction.id}>
              {showUser && <td><strong>{transaction.userName}</strong><br /><span className="muted mono">{transaction.userPhone}</span></td>}
              <td><span className={`badge ${credit ? "badge-credit" : "badge-debit"}`}>{credit ? <ArrowDownLeft aria-hidden="true" /> : <ArrowUpRight aria-hidden="true" />}{transaction.type}</span><div className="muted" style={{ marginTop: 4 }}>{transaction.description}</div></td>
              <td className="mono">{credit ? "+" : "−"}{money(transaction.amount)} AFN</td>
              <td>{dateTime(transaction.createdAt)}</td>
              <td>{transaction.status}</td>
            </tr>;
          })}
          {transactions.length === 0 && <tr><td className="empty-cell" colSpan={showUser ? 5 : 4}>No transactions to show.</td></tr>}
        </tbody>
      </table>
    </div>
  );
}