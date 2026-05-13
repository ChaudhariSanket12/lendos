import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { documentsApi } from '../api/documents'
import { loansApi } from '../api/loans'

const statusStyles = {
  APPLIED: 'bg-blue-100 text-blue-700',
  UNDER_ASSESSMENT: 'bg-yellow-100 text-yellow-800',
  APPROVED: 'bg-green-100 text-green-700',
  REJECTED: 'bg-red-100 text-red-700',
  DELETED: 'bg-gray-100 text-gray-700',
  DISBURSED: 'bg-purple-100 text-purple-700',
  ACTIVE: 'bg-green-100 text-green-700',
  CLOSED: 'bg-gray-100 text-gray-700',
  DEFAULTED: 'bg-red-100 text-red-700',
}

function getErrorMessage(err, fallback) {
  const backendMessage = err.response?.data?.message
  const status = err.response?.status
  if (backendMessage && status) return `${backendMessage} (HTTP ${status})`
  return backendMessage || err.message || fallback
}

function formatStatus(status) {
  return status ? status.replaceAll('_', ' ') : 'UNKNOWN'
}

function formatCurrency(amount) {
  if (amount === null || amount === undefined) return '-'
  return `₹${Number(amount).toLocaleString('en-IN', { maximumFractionDigits: 2 })}`
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString()
}

function formatDocumentType(type) {
  if (!type) return 'Document'
  return type === 'PAN' ? 'PAN Card' : type === 'AADHAAR' ? 'Aadhaar Card' : formatStatus(type)
}

function statusBadgeClass(status) {
  if (status === 'VERIFIED') return 'bg-green-100 text-green-700'
  if (status === 'REJECTED') return 'bg-red-100 text-red-700'
  return 'bg-gray-100 text-gray-700'
}

function getDocumentStatusText(status) {
  if (status === 'VERIFIED') return '✅ VERIFIED'
  if (status === 'REJECTED') return '❌ REJECTED'
  return '⏳ Awaiting Verification'
}

export default function LoanDetailPage() {
  const { loanId } = useParams()
  const navigate = useNavigate()
  const [loan, setLoan] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)
  const [successMessage, setSuccessMessage] = useState(null)
  const [showRejectModal, setShowRejectModal] = useState(false)
  const [showDeleteModal, setShowDeleteModal] = useState(false)
  const [rejectionReason, setRejectionReason] = useState('')
  const [finalMessage, setFinalMessage] = useState('')
  const [verifyingDocuments, setVerifyingDocuments] = useState({})
  const [verificationResults, setVerificationResults] = useState({})

  const loadLoan = async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await loansApi.getAdminLoanById(loanId)
      console.log('[LoanDetailPage] Loaded loan detail', data)
      setLoan(data)
    } catch (err) {
      console.error('[LoanDetailPage] Failed to load loan detail', err)
      setError(getErrorMessage(err, 'Failed to load loan detail.'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadLoan()
  }, [loanId])

  const displayStatus = loan?.isDeleted ? 'DELETED' : loan?.status

  const handleStatusUpdate = async (status, notes) => {
    setSaving(true)
    setError(null)
    setSuccessMessage(null)
    try {
      const updated = await loansApi.updateLoanStatus(loanId, { status, notes })
      setLoan(updated)
      setSuccessMessage(`Loan moved to ${formatStatus(status)}.`)
    } catch (err) {
      console.error('[LoanDetailPage] Failed to update loan status', err)
      setError(getErrorMessage(err, 'Failed to update loan status.'))
    } finally {
      setSaving(false)
    }
  }

  const handleReject = async () => {
    const reason = rejectionReason.trim()
    if (!reason) {
      setError('Rejection reason is required.')
      return
    }

    setSaving(true)
    setError(null)
    setSuccessMessage(null)
    try {
      const response = await loansApi.reject(loanId, reason)
      setShowRejectModal(false)
      setRejectionReason('')
      setSuccessMessage(response?.message || 'Application rejected. Documents deleted.')
      await loadLoan()
    } catch (err) {
      console.error('[LoanDetailPage] Failed to reject application', err)
      setError(getErrorMessage(err, 'Failed to reject application.'))
    } finally {
      setSaving(false)
    }
  }

  const handleDeleteApplication = async () => {
    setSaving(true)
    setError(null)
    setSuccessMessage(null)
    try {
      const response = await loansApi.deleteApplication(loanId, finalMessage.trim())
      setShowDeleteModal(false)
      setFinalMessage('')
      navigate('/loans', { state: { message: response?.message || 'Application deleted.' } })
    } catch (err) {
      console.error('[LoanDetailPage] Failed to delete application', err)
      setError(getErrorMessage(err, 'Failed to delete application.'))
    } finally {
      setSaving(false)
    }
  }

  const handleVerifyDocument = async (doc) => {
    if (!doc?.id) return

    setVerifyingDocuments((prev) => ({ ...prev, [doc.id]: true }))
    setError(null)
    setSuccessMessage(null)

    try {
      const response = await documentsApi.verifyDocument(doc.id)
      setVerificationResults((prev) => ({ ...prev, [doc.id]: response }))

      setLoan((prev) => {
        if (!prev?.borrower?.documents) return prev
        return {
          ...prev,
          borrower: {
            ...prev.borrower,
            documents: prev.borrower.documents.map((item) =>
              item.id === doc.id
                ? {
                    ...item,
                    verificationStatus: response.verificationStatus,
                    verifiedAt: response.verifiedAt,
                  }
                : item
            ),
          },
        }
      })

      setSuccessMessage(`${formatDocumentType(doc.documentType)} verification completed.`)
    } catch (err) {
      console.error('[LoanDetailPage] Failed to verify document', err)
      setError(getErrorMessage(err, 'Failed to verify document.'))
    } finally {
      setVerifyingDocuments((prev) => ({ ...prev, [doc.id]: false }))
    }
  }

  const renderActions = () => {
    if (!loan) return null

    if (loan.isDeleted) {
      return <p className="text-sm text-gray-600">This application is deleted from active records.</p>
    }

    if (loan.status === 'APPLIED') {
      return (
        <div className="flex flex-wrap gap-3">
          <button className="btn-primary" disabled={saving} onClick={() => handleStatusUpdate('UNDER_ASSESSMENT', 'Starting credit assessment')}>
            Start Assessment
          </button>
          <button className="btn-danger" disabled={saving} onClick={() => setShowRejectModal(true)}>
            Reject Application
          </button>
        </div>
      )
    }

    if (loan.status === 'UNDER_ASSESSMENT') {
      return (
        <div className="flex flex-wrap gap-3">
          <button className="btn-primary" disabled={saving} onClick={() => handleStatusUpdate('APPROVED', 'All checks passed')}>
            Approve
          </button>
          <button className="btn-danger" disabled={saving} onClick={() => setShowRejectModal(true)}>
            Reject Application
          </button>
        </div>
      )
    }

    if (loan.status === 'APPROVED') {
      return (
        <button className="btn-primary" disabled={saving} onClick={() => handleStatusUpdate('DISBURSED', 'Disbursement confirmed')}>
          Confirm Disbursement
        </button>
      )
    }

    if (loan.status === 'REJECTED') {
      return (
        <button className="btn-danger" disabled={saving} onClick={() => setShowDeleteModal(true)}>
          Delete Application
        </button>
      )
    }

    return <p className="text-sm text-gray-600">No manual actions available for current status.</p>
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-white shadow-sm px-6 py-4 flex items-center justify-between">
        <h1 className="text-lg font-bold text-blue-700">LendOS</h1>
        <button className="btn-secondary" onClick={() => navigate('/loans')}>
          Back to Loans
        </button>
      </nav>

      <div className="max-w-5xl mx-auto p-8 space-y-5">
        {loading && <div className="alert-info">Loading loan detail...</div>}
        {error && <div className="alert-error">{error}</div>}
        {successMessage && <div className="alert-success">{successMessage}</div>}

        {!loading && !error && loan && (
          <>
            <div className="card">
              <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
                <h2 className="text-xl font-semibold text-gray-800">Loan Details</h2>
                <span className={`inline-flex px-3 py-1 rounded text-xs font-medium ${statusStyles[displayStatus] || 'bg-gray-100 text-gray-700'}`}>
                  {formatStatus(displayStatus)}
                </span>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                <div>
                  <p className="text-gray-500">Loan Amount</p>
                  <p className="text-gray-800">{formatCurrency(loan.loanAmount)}</p>
                </div>
                <div>
                  <p className="text-gray-500">Tenure</p>
                  <p className="text-gray-800">{loan.tenureMonths} months</p>
                </div>
                <div>
                  <p className="text-gray-500">Loan Purpose</p>
                  <p className="text-gray-800">{formatStatus(loan.loanPurpose)}</p>
                </div>
                <div>
                  <p className="text-gray-500">Applied At</p>
                  <p className="text-gray-800">{formatDate(loan.appliedAt)}</p>
                </div>
                <div>
                  <p className="text-gray-500">Disbursement Date</p>
                  <p className="text-gray-800">{loan.disbursementDate || '-'}</p>
                </div>
                <div>
                  <p className="text-gray-500">Status Notes</p>
                  <p className="text-gray-800">{loan.statusNotes || '-'}</p>
                </div>
              </div>
            </div>

            {loan.status === 'REJECTED' && (
              <div className="card bg-red-50 border-red-200">
                <h3 className="text-lg font-semibold text-red-800 mb-3">
                  {loan.isDeleted ? 'Application Deleted' : 'Application Rejected'}
                </h3>
                <div className="text-sm text-red-900 space-y-1">
                  <p><span className="font-medium">Reason:</span> {loan.rejectionMessage || '-'}</p>
                  <p><span className="font-medium">Rejected At:</span> {formatDate(loan.rejectedAt)}</p>
                  <p><span className="font-medium">Rejected By:</span> {loan.rejectedByName || '-'}</p>
                  {loan.isDeleted && (
                    <>
                      <p><span className="font-medium">Deleted At:</span> {formatDate(loan.deletedAt)}</p>
                      <p><span className="font-medium">Deleted By:</span> {loan.deletedBy || '-'}</p>
                    </>
                  )}
                </div>
              </div>
            )}

            <div className="card">
              <h3 className="text-lg font-semibold text-gray-800 mb-3">Borrower Profile Summary</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                <div>
                  <p className="text-gray-500">Name</p>
                  <p className="text-gray-800">{loan.borrower?.fullName || '-'}</p>
                </div>
                <div>
                  <p className="text-gray-500">Borrower Status</p>
                  <p className="text-gray-800">{formatStatus(loan.borrower?.status)}</p>
                </div>
                <div>
                  <p className="text-gray-500">Email</p>
                  <p className="text-gray-800">{loan.borrower?.email || '-'}</p>
                </div>
                <div>
                  <p className="text-gray-500">Phone</p>
                  <p className="text-gray-800">{loan.borrower?.phone || '-'}</p>
                </div>
                <div>
                  <p className="text-gray-500">PAN</p>
                  <p className="text-gray-800">{loan.borrower?.panNumber || '-'}</p>
                </div>
                <div>
                  <p className="text-gray-500">Aadhaar</p>
                  <p className="text-gray-800">{loan.borrower?.aadhaarNumber || '-'}</p>
                </div>
              </div>
            </div>

            <div className="card">
              <h3 className="text-lg font-semibold text-gray-800 mb-3">Documents</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {['PAN', 'AADHAAR'].map((type) => {
                  const doc = loan.borrower?.documents?.find((item) => item.documentType === type)
                  const isVerifying = doc?.id ? Boolean(verifyingDocuments[doc.id]) : false
                  const verificationResult = doc?.id ? verificationResults[doc.id] : null
                  const currentStatus = doc?.verificationStatus || 'PENDING'
                  return (
                    <div key={type} className="border border-gray-200 rounded-lg p-4 bg-gray-50">
                      {doc?.documentUrl ? (
                        <div className="space-y-3">
                          <div className="flex items-center justify-between gap-2 mb-2">
                            <p className="text-sm font-medium text-gray-800">
                              📷 {formatDocumentType(type)} {currentStatus === 'VERIFIED' ? '✅' : ''}
                            </p>
                            <span className={`px-2 py-1 rounded text-xs font-medium ${statusBadgeClass(currentStatus)}`}>
                              {formatStatus(currentStatus)}
                            </span>
                          </div>

                          <a
                            href={doc.documentUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="block"
                          >
                            <img
                              src={doc.documentUrl}
                              alt={`${formatDocumentType(type)} thumbnail`}
                              className="w-28 h-20 object-cover rounded border border-gray-200"
                            />
                          </a>
                          <p className="text-xs text-gray-500">
                            Uploaded at: {formatDate(doc.uploadedAt)}
                          </p>
                          {isVerifying ? (
                            <div className="text-xs text-blue-700 bg-blue-50 border border-blue-100 rounded p-2">
                              <p>Status: 🔄 Verifying... (3-5 seconds)</p>
                              <p>Calling OCR API...</p>
                            </div>
                          ) : (
                            <p className="text-xs text-gray-700">Status: {getDocumentStatusText(currentStatus)}</p>
                          )}

                          <div className="flex flex-wrap gap-2">
                            <a
                              href={doc.documentUrl}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="btn btn-secondary"
                            >
                              View Full Image
                            </a>
                            <button
                              type="button"
                              className="btn btn-primary"
                              onClick={() => handleVerifyDocument(doc)}
                              disabled={isVerifying}
                            >
                              {isVerifying
                                ? 'Verifying...'
                                : currentStatus === 'PENDING'
                                ? '🔄 Verify Document'
                                : '🔄 Re-verify'}
                            </button>
                          </div>

                          {verificationResult && (
                            <div className="border border-gray-200 bg-white rounded p-3 space-y-2">
                              <p className="text-xs font-semibold text-gray-800">OCR Results</p>
                              {type === 'PAN' ? (
                                <>
                                  <p className="text-xs text-gray-700">
                                    Extracted PAN: {verificationResult.extractedData?.panNumber || '-'}
                                  </p>
                                  <p className="text-xs text-gray-700">
                                    Profile PAN: {verificationResult.profileData?.panNumber || '-'}
                                  </p>
                                  <p className="text-xs font-medium">
                                    {verificationResult.matches?.panMatch ? '✅ PAN Match' : '❌ PAN Mismatch!'}
                                  </p>
                                </>
                              ) : (
                                <>
                                  <p className="text-xs text-gray-700">
                                    Extracted Aadhaar: {verificationResult.extractedData?.aadhaarNumber || '-'}
                                  </p>
                                  <p className="text-xs text-gray-700">
                                    Profile Aadhaar: {verificationResult.profileData?.aadhaarNumber || '-'}
                                  </p>
                                  <p className="text-xs font-medium">
                                    {verificationResult.matches?.aadhaarMatch ? '✅ Aadhaar Match' : '❌ Aadhaar Mismatch!'}
                                  </p>
                                  <p className="text-xs text-gray-700">
                                    Extracted Name: {verificationResult.extractedData?.nameOnCard || '-'}
                                  </p>
                                  <p className="text-xs text-gray-700">
                                    Profile Name: {verificationResult.profileData?.fullName || '-'}
                                  </p>
                                  <p className="text-xs font-medium">
                                    {verificationResult.matches?.nameMatch ? '✅ Name Match' : '❌ Name Mismatch'}
                                  </p>
                                </>
                              )}

                              <p className="text-xs text-gray-700">
                                Status: {verificationResult.verificationStatus}
                              </p>
                              <p className="text-xs text-gray-700">
                                Verified at: {formatDate(verificationResult.verifiedAt)}
                              </p>
                              <div>
                                <p className="text-xs font-medium text-gray-700">OCR Text</p>
                                <pre className="text-[11px] whitespace-pre-wrap bg-gray-50 border border-gray-200 rounded p-2 mt-1 max-h-28 overflow-y-auto">
                                  {verificationResult.ocrText || '-'}
                                </pre>
                              </div>
                            </div>
                          )}
                        </div>
                      ) : (
                        <p className="text-sm text-gray-600">No document uploaded.</p>
                      )}
                    </div>
                  )
                })}
              </div>
            </div>

            <div className="card">
              <h3 className="text-lg font-semibold text-gray-800 mb-3">Financial Profile</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                <div>
                  <p className="text-gray-500">Monthly Income</p>
                  <p className="text-gray-800">{formatCurrency(loan.borrower?.monthlyIncome)}</p>
                </div>
                <div>
                  <p className="text-gray-500">Existing Obligations</p>
                  <p className="text-gray-800">{formatCurrency(loan.borrower?.existingMonthlyObligations)}</p>
                </div>
                <div>
                  <p className="text-gray-500">Employment Type</p>
                  <p className="text-gray-800">{formatStatus(loan.borrower?.employmentType)}</p>
                </div>
                <div>
                  <p className="text-gray-500">Years in Current Job</p>
                  <p className="text-gray-800">{loan.borrower?.yearsInCurrentJob ?? '-'}</p>
                </div>
                <div>
                  <p className="text-gray-500">Total Work Experience</p>
                  <p className="text-gray-800">{loan.borrower?.totalWorkExperience ?? '-'}</p>
                </div>
                <div>
                  <p className="text-gray-500">Residence Type</p>
                  <p className="text-gray-800">{formatStatus(loan.borrower?.residenceType)}</p>
                </div>
                <div>
                  <p className="text-gray-500">Years at Current Residence</p>
                  <p className="text-gray-800">{loan.borrower?.yearsAtCurrentResidence ?? '-'}</p>
                </div>
                <div>
                  <p className="text-gray-500">CIBIL Score</p>
                  <p className="text-gray-800">{loan.borrower?.cibilScore ?? '-'}</p>
                </div>
              </div>
            </div>

            {(loan.status !== 'APPLIED') && (
              <div className="card">
                <h3 className="text-lg font-semibold text-gray-800 mb-3">Risk Assessment</h3>
                {loan.risk ? (
                  <div className="grid grid-cols-1 md:grid-cols-4 gap-4 text-sm">
                    <div>
                      <p className="text-gray-500">Risk Score</p>
                      <p className="text-gray-800">{loan.risk.riskScore}/100</p>
                    </div>
                    <div>
                      <p className="text-gray-500">FOIR</p>
                      <p className="text-gray-800">{loan.risk.foir}%</p>
                    </div>
                    <div>
                      <p className="text-gray-500">Recommendation</p>
                      <p className="text-gray-800">{formatStatus(loan.risk.recommendation)}</p>
                    </div>
                    <div>
                      <p className="text-gray-500">Assessed At</p>
                      <p className="text-gray-800">{formatDate(loan.risk.assessedAt)}</p>
                    </div>
                  </div>
                ) : (
                  <p className="text-sm text-gray-600">Risk evaluation not available yet.</p>
                )}
              </div>
            )}

            <div className="card">
              <h3 className="text-lg font-semibold text-gray-800 mb-3">Actions</h3>
              {renderActions()}
            </div>
          </>
        )}
      </div>

      {showRejectModal && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-lg border border-gray-200 shadow-xl max-w-lg w-full p-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-3">❌ Reject Loan Application</h3>
            <div className="text-sm text-gray-700 space-y-1 mb-4">
              <p>This will:</p>
              <p>• Reject the loan application</p>
              <p>• Delete all uploaded documents</p>
              <p>• Remove document files from storage</p>
              <p className="font-medium text-red-700 mt-2">This action cannot be undone.</p>
            </div>
            <label className="label">Rejection Reason</label>
            <textarea
              className="input min-h-[100px]"
              value={rejectionReason}
              onChange={(event) => setRejectionReason(event.target.value)}
              placeholder="Income documentation insufficient. Please provide bank statements."
              disabled={saving}
            />
            <div className="mt-5 flex items-center justify-end gap-3">
              <button className="btn-secondary" disabled={saving} onClick={() => setShowRejectModal(false)}>
                Cancel
              </button>
              <button className="btn-danger" disabled={saving} onClick={handleReject}>
                {saving ? 'Rejecting...' : 'Confirm Rejection'}
              </button>
            </div>
          </div>
        </div>
      )}

      {showDeleteModal && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-lg border border-gray-200 shadow-xl max-w-lg w-full p-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-3">🗑️ Delete Loan Application</h3>
            <p className="text-sm text-gray-700 mb-4">
              This removes the application from active records. The borrower will still see the final message.
            </p>
            <label className="label">Final Message to Borrower</label>
            <textarea
              className="input min-h-[100px]"
              value={finalMessage}
              onChange={(event) => setFinalMessage(event.target.value)}
              placeholder="Application closed due to incomplete verification."
              disabled={saving}
            />
            <div className="mt-5 flex items-center justify-end gap-3">
              <button className="btn-secondary" disabled={saving} onClick={() => setShowDeleteModal(false)}>
                Cancel
              </button>
              <button className="btn-danger" disabled={saving} onClick={handleDeleteApplication}>
                {saving ? 'Deleting...' : 'Delete Application'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
