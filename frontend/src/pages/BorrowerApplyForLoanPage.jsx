import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import BorrowerLayout from '../components/BorrowerLayout'
import { borrowerPortalApi } from '../api/borrowerPortal'
import { loansApi } from '../api/loans'

const EMPLOYMENT_TYPES = ['SALARIED', 'GOVERNMENT', 'SELF_EMPLOYED', 'PROFESSIONAL', 'RETIRED', 'OTHER']
const LOAN_PURPOSES = [
  'DEBT_CONSOLIDATION',
  'HOME_RENOVATION',
  'MEDICAL',
  'EDUCATION',
  'BUSINESS',
  'WEDDING',
  'TRAVEL',
  'VEHICLE',
  'OTHER',
]
const RESIDENCE_TYPES = ['OWNED', 'RENTED', 'WITH_FAMILY', 'COMPANY_PROVIDED']
const TENURE_OPTIONS = [3, 6, 12, 18, 24, 36, 48, 60]

function getErrorMessage(err, fallback) {
  const backendMessage = err.response?.data?.message
  const status = err.response?.status
  if (backendMessage && status) return `${backendMessage} (HTTP ${status})`
  return backendMessage || err.message || fallback
}

function toLabel(value) {
  return value.replaceAll('_', ' ')
}

function toNumber(value) {
  if (value === '' || value === null || value === undefined) return null
  const number = Number(value)
  return Number.isNaN(number) ? null : number
}

function hasAtMostOneDecimal(value) {
  if (value === null || value === undefined || value === '') return true
  return /^-?\d+(\.\d)?$/.test(String(value))
}

function calculateEmi(loanAmount, tenureMonths) {
  if (!loanAmount || !tenureMonths) return null
  const p = Number(loanAmount)
  const n = Number(tenureMonths)
  if (!p || !n) return null
  const r = 0.01
  const numerator = p * r * Math.pow(1 + r, n)
  const denominator = Math.pow(1 + r, n) - 1
  if (denominator <= 0) return null
  return numerator / denominator
}

function validate(form) {
  const errors = {}
  const monthlyIncome = toNumber(form.monthlyIncome)
  const yearsInCurrentJob = toNumber(form.yearsInCurrentJob)
  const totalWorkExperience = toNumber(form.totalWorkExperience)
  const existingMonthlyObligations = toNumber(form.existingMonthlyObligations)
  const loanAmount = toNumber(form.loanAmount)
  const yearsAtCurrentResidence = toNumber(form.yearsAtCurrentResidence)
  const cibilScore = toNumber(form.cibilScore)
  const tenureMonths = toNumber(form.tenureMonths)

  if (monthlyIncome === null) {
    errors.monthlyIncome = 'Monthly income is required'
  } else if (monthlyIncome < 10000 || monthlyIncome > 10000000) {
    errors.monthlyIncome = 'Monthly income must be between 10,000 and 10,000,000'
  }

  if (!form.employmentType) {
    errors.employmentType = 'Employment type is required'
  }

  if (yearsInCurrentJob === null) {
    errors.yearsInCurrentJob = 'Years in current job is required'
  } else if (yearsInCurrentJob < 0 || yearsInCurrentJob > 50) {
    errors.yearsInCurrentJob = 'Years in current job must be between 0 and 50'
  } else if (!hasAtMostOneDecimal(form.yearsInCurrentJob)) {
    errors.yearsInCurrentJob = 'Use at most 1 decimal place'
  }

  if (totalWorkExperience === null) {
    errors.totalWorkExperience = 'Total work experience is required'
  } else if (totalWorkExperience < 0 || totalWorkExperience > 60) {
    errors.totalWorkExperience = 'Total work experience must be between 0 and 60'
  } else if (!hasAtMostOneDecimal(form.totalWorkExperience)) {
    errors.totalWorkExperience = 'Use at most 1 decimal place'
  } else if (yearsInCurrentJob !== null && totalWorkExperience < yearsInCurrentJob) {
    errors.totalWorkExperience = 'Total work experience must be ≥ years in current job'
  }

  if (existingMonthlyObligations === null) {
    errors.existingMonthlyObligations = 'Existing monthly obligations are required'
  } else if (existingMonthlyObligations < 0) {
    errors.existingMonthlyObligations = 'Existing monthly obligations cannot be negative'
  } else if (monthlyIncome !== null && existingMonthlyObligations > monthlyIncome) {
    errors.existingMonthlyObligations = 'Existing obligations cannot exceed monthly income'
  }

  if (loanAmount === null) {
    errors.loanAmount = 'Loan amount is required'
  } else if (loanAmount < 5000 || loanAmount > 5000000) {
    errors.loanAmount = 'Loan amount must be between ₹5,000 and ₹50,00,000'
  }

  if (!tenureMonths) {
    errors.tenureMonths = 'Tenure is required'
  } else if (!TENURE_OPTIONS.includes(tenureMonths)) {
    errors.tenureMonths = 'Invalid tenure selected'
  }

  if (!form.loanPurpose) {
    errors.loanPurpose = 'Loan purpose is required'
  }

  if (!form.residenceType) {
    errors.residenceType = 'Residence type is required'
  }

  if (yearsAtCurrentResidence === null) {
    errors.yearsAtCurrentResidence = 'Years at current residence is required'
  } else if (yearsAtCurrentResidence < 0 || yearsAtCurrentResidence > 50) {
    errors.yearsAtCurrentResidence = 'Years at current residence must be between 0 and 50'
  } else if (!hasAtMostOneDecimal(form.yearsAtCurrentResidence)) {
    errors.yearsAtCurrentResidence = 'Use at most 1 decimal place'
  }

  if (form.cibilScore !== '' && (cibilScore === null || cibilScore < 300 || cibilScore > 900)) {
    errors.cibilScore = 'CIBIL score must be between 300 and 900'
  }

  return errors
}

export default function BorrowerApplyForLoanPage() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)
  const [submitted, setSubmitted] = useState(false)
  const [touched, setTouched] = useState({})
  const [backendErrors, setBackendErrors] = useState({})
  const [form, setForm] = useState({
    monthlyIncome: '',
    employmentType: 'SALARIED',
    yearsInCurrentJob: '',
    totalWorkExperience: '',
    existingMonthlyObligations: '',
    loanAmount: '',
    tenureMonths: '36',
    loanPurpose: '',
    residenceType: 'OWNED',
    yearsAtCurrentResidence: '',
    cibilScore: '',
  })

  const clientErrors = useMemo(() => validate(form), [form])
  const isValid = Object.keys(clientErrors).length === 0
  const estimatedEmi = useMemo(
    () => calculateEmi(toNumber(form.loanAmount), toNumber(form.tenureMonths)),
    [form.loanAmount, form.tenureMonths]
  )

  useEffect(() => {
    const loadProfile = async () => {
      setLoading(true)
      setError(null)
      try {
        const profile = await borrowerPortalApi.getProfile()
        console.log('[BorrowerApplyForLoanPage] Loaded profile', profile)
        setForm((prev) => ({
          ...prev,
          monthlyIncome: profile.monthlyIncome ?? '',
          employmentType: profile.employmentType || 'SALARIED',
          yearsInCurrentJob: profile.yearsInCurrentJob ?? '',
          totalWorkExperience: profile.totalWorkExperience ?? '',
          existingMonthlyObligations: profile.existingMonthlyObligations ?? '',
          residenceType: profile.residenceType || 'OWNED',
          yearsAtCurrentResidence: profile.yearsAtCurrentResidence ?? '',
          cibilScore: profile.cibilScore ?? '',
        }))
      } catch (err) {
        console.error('[BorrowerApplyForLoanPage] Failed to load profile', err)
        setError(getErrorMessage(err, 'Failed to load profile.'))
      } finally {
        setLoading(false)
      }
    }
    loadProfile()
  }, [])

  const onChange = (field) => (e) => {
    const value = e.target.value
    setForm((prev) => ({ ...prev, [field]: value }))
    setBackendErrors((prev) => {
      if (!prev[field]) return prev
      const next = { ...prev }
      delete next[field]
      return next
    })
  }

  const onBlur = (field) => () => setTouched((prev) => ({ ...prev, [field]: true }))

  const fieldError = (field) => {
    if (backendErrors[field]) return backendErrors[field]
    if (submitted || touched[field]) return clientErrors[field]
    return null
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSubmitted(true)
    setError(null)
    setBackendErrors({})
    if (!isValid) return

    const payload = {
      monthlyIncome: toNumber(form.monthlyIncome),
      employmentType: form.employmentType,
      yearsInCurrentJob: toNumber(form.yearsInCurrentJob),
      totalWorkExperience: toNumber(form.totalWorkExperience),
      existingMonthlyObligations: toNumber(form.existingMonthlyObligations),
      loanAmount: toNumber(form.loanAmount),
      tenureMonths: toNumber(form.tenureMonths),
      loanPurpose: form.loanPurpose,
      residenceType: form.residenceType,
      yearsAtCurrentResidence: toNumber(form.yearsAtCurrentResidence),
      cibilScore: form.cibilScore === '' ? null : toNumber(form.cibilScore),
    }

    setSaving(true)
    try {
      console.log('[BorrowerApplyForLoanPage] apply payload', payload)
      await loansApi.apply(payload)
      navigate('/borrower/my-loans', { state: { message: 'Loan application submitted successfully!' } })
    } catch (err) {
      console.error('[BorrowerApplyForLoanPage] Failed to submit loan application', err)
      const errors = err.response?.data?.errors
      if (errors && typeof errors === 'object') {
        setBackendErrors(errors)
      }
      setError(getErrorMessage(err, 'Failed to submit loan application.'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <BorrowerLayout title="💰 Apply for Loan" subtitle="Step 3 of 3: Financial & Loan Details">
      {loading ? (
        <div className="alert-info">Loading financial profile...</div>
      ) : (
        <div className="max-w-4xl space-y-5">
          {error && <div className="alert-error">{error}</div>}

          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="card">
              <h3 className="text-lg font-semibold text-gray-800 mb-4">Financial Profile</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="label">Monthly Income (₹)</label>
                  <input className="input" type="number" min="10000" step="0.01" value={form.monthlyIncome} onChange={onChange('monthlyIncome')} onBlur={onBlur('monthlyIncome')} />
                  <p className="text-xs text-gray-500 mt-1">Your total monthly income after tax.</p>
                  {fieldError('monthlyIncome') && <p className="text-xs text-red-600 mt-1">{fieldError('monthlyIncome')}</p>}
                </div>

                <div>
                  <label className="label">Employment Type</label>
                  <select className="input" value={form.employmentType} onChange={onChange('employmentType')} onBlur={onBlur('employmentType')}>
                    {EMPLOYMENT_TYPES.map((option) => (
                      <option key={option} value={option}>{toLabel(option)}</option>
                    ))}
                  </select>
                  {fieldError('employmentType') && <p className="text-xs text-red-600 mt-1">{fieldError('employmentType')}</p>}
                </div>

                <div>
                  <label className="label">Years in Current Job</label>
                  <input className="input" type="number" min="0" max="50" step="0.1" value={form.yearsInCurrentJob} onChange={onChange('yearsInCurrentJob')} onBlur={onBlur('yearsInCurrentJob')} />
                  <p className="text-xs text-gray-500 mt-1">For example, 4.5 for 4 years 6 months.</p>
                  {fieldError('yearsInCurrentJob') && <p className="text-xs text-red-600 mt-1">{fieldError('yearsInCurrentJob')}</p>}
                </div>

                <div>
                  <label className="label">Total Work Experience</label>
                  <input className="input" type="number" min="0" max="60" step="0.1" value={form.totalWorkExperience} onChange={onChange('totalWorkExperience')} onBlur={onBlur('totalWorkExperience')} />
                  <p className="text-xs text-gray-500 mt-1">Must be greater than or equal to years in current job.</p>
                  {fieldError('totalWorkExperience') && <p className="text-xs text-red-600 mt-1">{fieldError('totalWorkExperience')}</p>}
                </div>

                <div>
                  <label className="label">Existing Monthly Obligations (₹)</label>
                  <input className="input" type="number" min="0" step="0.01" value={form.existingMonthlyObligations} onChange={onChange('existingMonthlyObligations')} onBlur={onBlur('existingMonthlyObligations')} />
                  <p className="text-xs text-gray-500 mt-1">Current EMIs, rent, and other fixed obligations.</p>
                  {fieldError('existingMonthlyObligations') && <p className="text-xs text-red-600 mt-1">{fieldError('existingMonthlyObligations')}</p>}
                </div>

                <div>
                  <label className="label">Residence Type</label>
                  <select className="input" value={form.residenceType} onChange={onChange('residenceType')} onBlur={onBlur('residenceType')}>
                    {RESIDENCE_TYPES.map((option) => (
                      <option key={option} value={option}>{toLabel(option)}</option>
                    ))}
                  </select>
                  {fieldError('residenceType') && <p className="text-xs text-red-600 mt-1">{fieldError('residenceType')}</p>}
                </div>

                <div>
                  <label className="label">Years at Current Residence</label>
                  <input className="input" type="number" min="0" max="50" step="0.1" value={form.yearsAtCurrentResidence} onChange={onChange('yearsAtCurrentResidence')} onBlur={onBlur('yearsAtCurrentResidence')} />
                  {fieldError('yearsAtCurrentResidence') && <p className="text-xs text-red-600 mt-1">{fieldError('yearsAtCurrentResidence')}</p>}
                </div>

                <div>
                  <label className="label">CIBIL Score (Optional)</label>
                  <input className="input" type="number" min="300" max="900" step="1" value={form.cibilScore} onChange={onChange('cibilScore')} onBlur={onBlur('cibilScore')} />
                  <p className="text-xs text-gray-500 mt-1">If you know your score, provide a value between 300 and 900.</p>
                  {fieldError('cibilScore') && <p className="text-xs text-red-600 mt-1">{fieldError('cibilScore')}</p>}
                </div>
              </div>
            </div>

            <div className="card">
              <h3 className="text-lg font-semibold text-gray-800 mb-4">Loan Details</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="label">Loan Amount (₹)</label>
                  <input className="input" type="number" min="5000" max="5000000" step="0.01" value={form.loanAmount} onChange={onChange('loanAmount')} onBlur={onBlur('loanAmount')} />
                  <p className="text-xs text-gray-500 mt-1">₹5,000 - ₹50,00,000</p>
                  {fieldError('loanAmount') && <p className="text-xs text-red-600 mt-1">{fieldError('loanAmount')}</p>}
                </div>

                <div>
                  <label className="label">Tenure</label>
                  <select className="input" value={form.tenureMonths} onChange={onChange('tenureMonths')} onBlur={onBlur('tenureMonths')}>
                    {TENURE_OPTIONS.map((months) => (
                      <option key={months} value={months}>{months} months</option>
                    ))}
                  </select>
                  {fieldError('tenureMonths') && <p className="text-xs text-red-600 mt-1">{fieldError('tenureMonths')}</p>}
                </div>

                <div className="md:col-span-2">
                  <label className="label">Loan Purpose</label>
                  <select className="input" value={form.loanPurpose} onChange={onChange('loanPurpose')} onBlur={onBlur('loanPurpose')}>
                    <option value="">Select Purpose</option>
                    {LOAN_PURPOSES.map((purpose) => (
                      <option key={purpose} value={purpose}>{toLabel(purpose)}</option>
                    ))}
                  </select>
                  {fieldError('loanPurpose') && <p className="text-xs text-red-600 mt-1">{fieldError('loanPurpose')}</p>}
                </div>
              </div>

              <div className="mt-5 p-4 rounded border border-blue-200 bg-blue-50">
                <p className="text-sm font-semibold text-blue-900">
                  💡 Estimated EMI: {estimatedEmi ? `₹${Math.round(estimatedEmi).toLocaleString('en-IN')}/month` : '-'}
                </p>
                <p className="text-xs text-blue-700 mt-1">
                  Calculated at 12% reducing annual rate (1% monthly).
                </p>
              </div>

              <div className="flex items-center justify-between mt-6">
                <button type="button" className="btn-secondary" onClick={() => navigate('/borrower/dashboard')} disabled={saving}>
                  ← Back
                </button>
                <button type="submit" className="btn-primary" disabled={saving || !isValid}>
                  {saving ? 'Submitting...' : 'Submit Application →'}
                </button>
              </div>
            </div>
          </form>
        </div>
      )}
    </BorrowerLayout>
  )
}
