import BorrowerLayout from '../components/BorrowerLayout'

export default function BorrowerMyLoansPage() {
  return (
    <BorrowerLayout title="My Loans" subtitle="Track your loan applications and active loans">
      <div className="card">
        <h3 className="text-lg font-semibold text-gray-800">No loans yet</h3>
        <p className="text-sm text-gray-500 mt-2">
          You have not applied for any loans yet. Your loans will appear here.
        </p>
      </div>
    </BorrowerLayout>
  )
}
