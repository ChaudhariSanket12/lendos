import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { loansApi } from '../api/loans';
import { documentsApi } from '../api/documents';
import DocumentUploader from '../components/DocumentUploader';
import '../styles/BorrowerApplyForLoanPage.css';

const BorrowerApplyForLoanPage = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [draftLoaded, setDraftLoaded] = useState(false);
  const [documentsLoading, setDocumentsLoading] = useState(true);
  const [uploadedDocuments, setUploadedDocuments] = useState({});

  const [formData, setFormData] = useState({
    // Income & Employment Section
    monthlyIncome: '',
    employmentType: 'SALARIED',
    employerName: '',
    industryType: '',
    yearsInCurrentJob: '',
    totalWorkExperience: '',
    salaryPaymentMode: 'BANK_TRANSFER',

    // Monthly Obligations Section
    rentExpense: '',
    existingLoanEmis: '',
    creditCardPayments: '',
    otherFixedExpenses: '',

    // Loan Request Section
    loanAmount: '',
    tenureMonths: '36',
    loanPurpose: '',
  });

  const [errors, setErrors] = useState({});
  const [notification, setNotification] = useState(null);
  const draftStorageKey = 'loanDraft_borrower';

  // Load existing documents
  useEffect(() => {
    const loadDocuments = async () => {
      setDocumentsLoading(true);
      try {
        const documents = await documentsApi.getAll().catch((error) => {
          console.error('[BorrowerApplyForLoanPage] Failed to load existing documents', error);
          return [];
        });

        const documentMap = {};
        if (Array.isArray(documents)) {
          documents.forEach((doc) => {
            if (doc?.documentType) {
              documentMap[doc.documentType] = doc;
            }
          });
        }
        setUploadedDocuments(documentMap);
      } catch (error) {
        console.error('[BorrowerApplyForLoanPage] Failed to load documents', error);
      } finally {
        setDocumentsLoading(false);
      }
    };

    loadDocuments();
  }, []);

  // Load draft from localStorage on mount
  useEffect(() => {
    const draft = localStorage.getItem(draftStorageKey);
    if (draft) {
      try {
        setFormData(JSON.parse(draft));
      } catch (err) {
        console.error('Failed to load draft:', err);
      }
    }
    setDraftLoaded(true);
  }, [draftStorageKey]);

  // Auto-save draft to localStorage whenever form changes
  useEffect(() => {
    if (draftLoaded) {
      localStorage.setItem(draftStorageKey, JSON.stringify(formData));
    }
  }, [formData, draftLoaded, draftStorageKey]);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  // Calculated values
  const monthlyIncomeNum = parseFloat(formData.monthlyIncome) || 0;
  const rentNum = parseFloat(formData.rentExpense) || 0;
  const loanEmisNum = parseFloat(formData.existingLoanEmis) || 0;
  const creditCardNum = parseFloat(formData.creditCardPayments) || 0;
  const otherExpensesNum = parseFloat(formData.otherFixedExpenses) || 0;
  const loanAmountNum = parseFloat(formData.loanAmount) || 0;
  const tenureMonthsNum = parseInt(formData.tenureMonths) || 0;

  const totalObligations = rentNum + loanEmisNum + creditCardNum + otherExpensesNum;
  const disposableIncome = monthlyIncomeNum - totalObligations;

  // EMI calculation: EMI = P × r × (1+r)^n / [(1+r)^n - 1]
  // where r = 0.01 (1% monthly = 12% annual reducing)
  const calculateEMI = useCallback((principal, months) => {
    if (principal <= 0 || months <= 0) return 0;
    const r = 0.01;
    const numerator = principal * r * Math.pow(1 + r, months);
    const denominator = Math.pow(1 + r, months) - 1;
    return numerator / denominator;
  }, []);

  const estimatedEMI = calculateEMI(loanAmountNum, tenureMonthsNum);
  const totalObligationsAfterLoan = totalObligations + estimatedEMI;
  const foirPercentage = monthlyIncomeNum > 0 
    ? (totalObligationsAfterLoan / monthlyIncomeNum) * 100 
    : 0;
  const remainingAfterEMI = disposableIncome - estimatedEMI;

  // Affordability status
  const getAffordabilityStatus = () => {
    if (foirPercentage < 40) return { status: '✅ Good', color: '#4CAF50', text: 'FOIR < 40% - Good' };
    if (foirPercentage <= 50) return { status: '⚠️ Borderline', color: '#FF9800', text: 'FOIR 40-50% - Borderline' };
    return { status: '❌ May not qualify', color: '#F44336', text: 'FOIR > 50% - May not qualify' };
  };

  const affordability = getAffordabilityStatus();

  const validateForm = () => {
    const newErrors = {};

    // Required fields validation
    if (!formData.monthlyIncome || parseFloat(formData.monthlyIncome) <= 0) {
      newErrors.monthlyIncome = 'Monthly income is required and must be > 0';
    }
    if (!formData.employerName || formData.employerName.trim().length < 2) {
      newErrors.employerName = 'Employer name is required (min 2 characters)';
    }
    if (!formData.industryType) {
      newErrors.industryType = 'Industry type is required';
    }
    if (!formData.yearsInCurrentJob || parseFloat(formData.yearsInCurrentJob) < 0) {
      newErrors.yearsInCurrentJob = 'Years in current job is required and must be >= 0';
    }
    if (!formData.totalWorkExperience || parseFloat(formData.totalWorkExperience) < 0) {
      newErrors.totalWorkExperience = 'Total work experience is required and must be >= 0';
    }
    if (!formData.salaryPaymentMode) {
      newErrors.salaryPaymentMode = 'Salary payment mode is required';
    }
    if (!formData.loanAmount || parseFloat(formData.loanAmount) <= 0) {
      newErrors.loanAmount = 'Loan amount is required and must be > 0';
    }
    const loanAmt = parseFloat(formData.loanAmount);
    if (loanAmt < 5000 || loanAmt > 5000000) {
      newErrors.loanAmount = 'Loan amount must be between ₹5,000 and ₹50,00,000';
    }
    if (!formData.loanPurpose) {
      newErrors.loanPurpose = 'Loan purpose is required';
    }

    // Obligation validation
    if (totalObligations > monthlyIncomeNum) {
      newErrors.existingLoanEmis = 'Total obligations cannot exceed monthly income';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSaveDraft = () => {
    localStorage.setItem(draftStorageKey, JSON.stringify(formData));
    showNotification('Draft saved successfully', 'success');
  };

  const showNotification = (message, type = 'info') => {
    setNotification({ message, type });
    setTimeout(() => setNotification(null), 3000);
  };

  const handleDocumentUploaded = (metadata) => {
    if (!metadata?.documentType) return;

    if (metadata.removed) {
      setUploadedDocuments((prev) => {
        const next = { ...prev };
        delete next[metadata.documentType];
        return next;
      });
      showNotification(`${metadata.documentType} document removed`, 'info');
      return;
    }

    setUploadedDocuments((prev) => ({
      ...prev,
      [metadata.documentType]: metadata,
    }));
    showNotification(`${metadata.documentType} document uploaded successfully`, 'success');
  };

  const handleDocumentError = (error) => {
    showNotification(error?.message || 'Document upload failed', 'error');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!validateForm()) {
      showNotification('Please correct the errors in the form', 'error');
      return;
    }

    setLoading(true);
    try {
      const documentUrls = Object.entries(uploadedDocuments).reduce((acc, [type, doc]) => {
        const url = doc?.documentUrl || doc?.url;
        if (url) {
          acc[type] = url;
        }
        return acc;
      }, {});

      const payload = {
        monthlyIncome: parseFloat(formData.monthlyIncome),
        employmentType: formData.employmentType,
        employerName: formData.employerName,
        industryType: formData.industryType,
        yearsInCurrentJob: parseInt(formData.yearsInCurrentJob),
        totalWorkExperience: parseInt(formData.totalWorkExperience),
        salaryPaymentMode: formData.salaryPaymentMode,
        rentExpense: parseFloat(formData.rentExpense) || 0,
        existingLoanEmis: parseFloat(formData.existingLoanEmis) || 0,
        creditCardPayments: parseFloat(formData.creditCardPayments) || 0,
        otherFixedExpenses: parseFloat(formData.otherFixedExpenses) || 0,
        loanAmount: parseFloat(formData.loanAmount),
        tenureMonths: parseInt(formData.tenureMonths),
        loanPurpose: loanPurposeMapping[formData.loanPurpose],
        documentUrls,
        residenceType: 'OWNED',
        yearsAtCurrentResidence: 1,
        cibilScore: 750,
      };

      await loansApi.apply(payload);
      
      // Clear draft on successful submission
      localStorage.removeItem(draftStorageKey);
      
      showNotification('Loan application submitted successfully', 'success');
      setTimeout(() => navigate('/borrower/my-loans'), 1500);
    } catch (error) {
      console.error('Error submitting loan application:', error);
      showNotification(error.message || 'Failed to submit loan application', 'error');
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (value) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value);
  };

  const employmentTypes = [
    { value: 'SALARIED', label: 'Salaried' },
    { value: 'SELF_EMPLOYED', label: 'Self Employed' },
    { value: 'BUSINESS_OWNER', label: 'Business Owner' },
  ];

  const industryTypes = [
    'Information Technology',
    'Manufacturing',
    'Banking',
    'Healthcare',
    'Education',
    'Retail',
    'Construction',
    'Other',
  ];

  const loanPurposeMapping = {
    'Home Improvement': 'HOME_RENOVATION',
    Medical: 'MEDICAL',
    Education: 'EDUCATION',
    Travel: 'TRAVEL',
    Vehicle: 'VEHICLE',
    Business: 'BUSINESS',
    'Debt Consolidation': 'DEBT_CONSOLIDATION',
    Wedding: 'WEDDING',
    Other: 'OTHER',
  };

  const loanPurposes = [
    'Home Improvement',
    'Education',
    'Wedding',
    'Medical',
    'Travel',
    'Vehicle',
    'Debt Consolidation',
    'Business',
    'Other',
  ];

  const salaryModes = [
    { value: 'BANK_TRANSFER', label: 'Bank Transfer' },
    { value: 'CHEQUE', label: 'Cheque' },
    { value: 'CASH', label: 'Cash' },
  ];

  return (
    <div className="borrower-apply-loan-page">
      {notification && (
        <div className={`notification notification-${notification.type}`}>
          {notification.message}
        </div>
      )}
      <div className="apply-loan-container">
        {/* Step Indicator */}
        <div className="step-indicator">
          <div className="step completed">
            <span className="step-number">1</span>
            <span className="step-label">Identity</span>
          </div>
          <div className="step-divider"></div>
          <div className="step completed">
            <span className="step-number">2</span>
            <span className="step-label">Documents</span>
          </div>
          <div className="step-divider"></div>
          <div className="step active">
            <span className="step-number">3</span>
            <span className="step-label">Loan Application</span>
          </div>
        </div>

        <form onSubmit={handleSubmit}>
          {/* Section 1: Income & Employment */}
          <div className="form-section">
            <div className="section-header">
              <span className="section-icon">💼</span>
              <h2>Income & Employment</h2>
            </div>

            <div className="form-group">
              <label htmlFor="monthlyIncome">
                Monthly Net Income (₹) <span className="required">*</span>
              </label>
              <input
                type="number"
                id="monthlyIncome"
                name="monthlyIncome"
                value={formData.monthlyIncome}
                onChange={handleInputChange}
                placeholder="Take-home salary after deductions"
                min="0"
                step="1000"
              />
              <small className="helper-text">Take-home salary after all deductions</small>
              {errors.monthlyIncome && <span className="error">{errors.monthlyIncome}</span>}
            </div>

            <div className="form-row">
              <div className="form-group">
                <label htmlFor="employmentType">
                  Employment Type <span className="required">*</span>
                </label>
                <select
                  id="employmentType"
                  name="employmentType"
                  value={formData.employmentType}
                  onChange={handleInputChange}
                >
                  {employmentTypes.map(type => (
                    <option key={type.value} value={type.value}>{type.label}</option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label htmlFor="salaryPaymentMode">
                  Salary Payment Mode <span className="required">*</span>
                </label>
                <div className="radio-group">
                  {salaryModes.map(mode => (
                    <label key={mode.value} className="radio-label">
                      <input
                        type="radio"
                        name="salaryPaymentMode"
                        value={mode.value}
                        checked={formData.salaryPaymentMode === mode.value}
                        onChange={handleInputChange}
                      />
                      {mode.label}
                    </label>
                  ))}
                </div>
                {errors.salaryPaymentMode && <span className="error">{errors.salaryPaymentMode}</span>}
              </div>
            </div>

            <div className="form-group">
              <label htmlFor="employerName">
                Employer/Company Name <span className="required">*</span>
              </label>
              <input
                type="text"
                id="employerName"
                name="employerName"
                value={formData.employerName}
                onChange={handleInputChange}
                placeholder="Your employer name"
              />
              {errors.employerName && <span className="error">{errors.employerName}</span>}
            </div>

            <div className="form-group">
              <label htmlFor="industryType">
                Industry Type <span className="required">*</span>
              </label>
              <select
                id="industryType"
                name="industryType"
                value={formData.industryType}
                onChange={handleInputChange}
              >
                <option value="">Select Industry</option>
                {industryTypes.map(industry => (
                  <option key={industry} value={industry}>{industry}</option>
                ))}
              </select>
              {errors.industryType && <span className="error">{errors.industryType}</span>}
            </div>

            <div className="form-row">
              <div className="form-group">
                <label htmlFor="yearsInCurrentJob">
                  Years in Current Job <span className="required">*</span>
                </label>
                <input
                  type="number"
                  id="yearsInCurrentJob"
                  name="yearsInCurrentJob"
                  value={formData.yearsInCurrentJob}
                  onChange={handleInputChange}
                  min="0"
                  step="0.5"
                  placeholder="0"
                />
                {errors.yearsInCurrentJob && <span className="error">{errors.yearsInCurrentJob}</span>}
              </div>

              <div className="form-group">
                <label htmlFor="totalWorkExperience">
                  Total Work Experience <span className="required">*</span>
                </label>
                <input
                  type="number"
                  id="totalWorkExperience"
                  name="totalWorkExperience"
                  value={formData.totalWorkExperience}
                  onChange={handleInputChange}
                  min="0"
                  step="0.5"
                  placeholder="0"
                />
                {errors.totalWorkExperience && <span className="error">{errors.totalWorkExperience}</span>}
              </div>
            </div>
          </div>

          {/* Section 2: Monthly Obligations */}
          <div className="form-section">
            <div className="section-header">
              <span className="section-icon">💳</span>
              <h2>Monthly Obligations (Self-Declared)</h2>
            </div>

            <div className="form-group">
              <label htmlFor="rentExpense">
                Rent/Mortgage (₹)
              </label>
              <input
                type="number"
                id="rentExpense"
                name="rentExpense"
                value={formData.rentExpense}
                onChange={handleInputChange}
                placeholder="0"
                min="0"
                step="1000"
              />
              <small className="helper-text">Enter 0 if not applicable</small>
            </div>

            <div className="form-group">
              <label htmlFor="existingLoanEmis">
                Existing Loan EMIs (₹)
              </label>
              <input
                type="number"
                id="existingLoanEmis"
                name="existingLoanEmis"
                value={formData.existingLoanEmis}
                onChange={handleInputChange}
                placeholder="0"
                min="0"
                step="1000"
              />
              <small className="helper-text">Total of all current loan payments</small>
              {errors.existingLoanEmis && <span className="error">{errors.existingLoanEmis}</span>}
            </div>

            <div className="form-group">
              <label htmlFor="creditCardPayments">
                Credit Card Payments (₹)
              </label>
              <input
                type="number"
                id="creditCardPayments"
                name="creditCardPayments"
                value={formData.creditCardPayments}
                onChange={handleInputChange}
                placeholder="0"
                min="0"
                step="1000"
              />
              <small className="helper-text">Average monthly credit card bill</small>
            </div>

            <div className="form-group">
              <label htmlFor="otherFixedExpenses">
                Other Fixed Expenses (₹)
              </label>
              <input
                type="number"
                id="otherFixedExpenses"
                name="otherFixedExpenses"
                value={formData.otherFixedExpenses}
                onChange={handleInputChange}
                placeholder="0"
                min="0"
                step="1000"
              />
              <small className="helper-text">Insurance, school fees, etc.</small>
            </div>

            {/* Summary Card */}
            <div className="obligations-summary">
              <div className="summary-row">
                <span className="summary-label">💰 Total Obligations:</span>
                <span className="summary-value">{formatCurrency(totalObligations)}/month</span>
              </div>
              <div className="summary-divider"></div>
              <div className="summary-row">
                <span className="summary-label">📊 Your Disposable Income:</span>
                <span className={`summary-value ${disposableIncome < 0 ? 'negative' : ''}`}>
                  {formatCurrency(disposableIncome)}/month
                </span>
              </div>
            </div>
          </div>

          {/* Section 3: Loan Request */}
          <div className="form-section">
            <div className="section-header">
              <span className="section-icon">🏦</span>
              <h2>Loan Request</h2>
            </div>

            <div className="form-group">
              <label htmlFor="loanAmount">
                Loan Amount (₹) <span className="required">*</span>
              </label>
              <input
                type="number"
                id="loanAmount"
                name="loanAmount"
                value={formData.loanAmount}
                onChange={handleInputChange}
                placeholder="₹5,000 - ₹50,00,000"
                min="5000"
                max="5000000"
                step="100"
              />
              <small className="helper-text">₹5,000 - ₹50,00,000</small>
              {errors.loanAmount && <span className="error">{errors.loanAmount}</span>}
            </div>

            <div className="form-row">
              <div className="form-group">
                <label htmlFor="tenureMonths">
                  Tenure (Months) <span className="required">*</span>
                </label>
                <select
                  id="tenureMonths"
                  name="tenureMonths"
                  value={formData.tenureMonths}
                  onChange={handleInputChange}
                >
                  <option value="3">3 months</option>
                  <option value="6">6 months</option>
                  <option value="12">12 months</option>
                  <option value="18">18 months</option>
                  <option value="24">24 months</option>
                  <option value="36">36 months</option>
                  <option value="48">48 months</option>
                  <option value="60">60 months</option>
                </select>
              </div>

              <div className="form-group">
                <label htmlFor="loanPurpose">
                  Loan Purpose <span className="required">*</span>
                </label>
                <select
                  id="loanPurpose"
                  name="loanPurpose"
                  value={formData.loanPurpose}
                  onChange={handleInputChange}
                >
                  <option value="">Select Purpose</option>
                  {loanPurposes.map(purpose => (
                    <option key={purpose} value={purpose}>{purpose}</option>
                  ))}
                </select>
                {errors.loanPurpose && <span className="error">{errors.loanPurpose}</span>}
              </div>
            </div>

            {/* Affordability Analysis Card */}
            <div className="affordability-card">
              <h3>📊 Affordability Analysis</h3>
              <div className="affordability-row">
                <span className="label">Estimated EMI:</span>
                <span className="value">{formatCurrency(estimatedEMI)}/month</span>
                <small>(Calculated at 12% p.a. reducing)</small>
              </div>
              <div className="affordability-row">
                <span className="label">FOIR After Loan:</span>
                <span className="value">{foirPercentage.toFixed(2)}%</span>
                <small>(Total Obligations + EMI) / Income</small>
              </div>
              <div className="affordability-row">
                <span className="label">Affordability Status:</span>
                <span className="status" style={{ color: affordability.color }}>
                  {affordability.status}
                </span>
                <small>{affordability.text}</small>
              </div>
              <div className="affordability-row">
                <span className="label">Remaining After EMI:</span>
                <span className={`value ${remainingAfterEMI < 0 ? 'negative' : ''}`}>
                  {formatCurrency(remainingAfterEMI)}/month
                </span>
              </div>
            </div>
          </div>

          {/* Section 4: Document Upload */}
          <div className="form-section document-section">
            <div className="section-header">
              <span className="section-icon">📋</span>
              <h2>Document Upload</h2>
            </div>
            <p className="section-description">
              Please upload clear photos of your identity documents. These documents are verified
              through OCR + manual review to speed up loan approval.
            </p>

            {documentsLoading ? (
              <div className="document-loading-card">Loading document context...</div>
            ) : (
              <div className="document-grid">
                <DocumentUploader
                  documentType="PAN"
                  existingDocument={uploadedDocuments.PAN}
                  onUploadComplete={handleDocumentUploaded}
                  onUploadError={handleDocumentError}
                />
                <DocumentUploader
                  documentType="AADHAAR"
                  existingDocument={uploadedDocuments.AADHAAR}
                  onUploadComplete={handleDocumentUploaded}
                  onUploadError={handleDocumentError}
                />
              </div>
            )}

            <div className="document-info-banner">
              <span className="info-icon">ℹ️</span>
              <p>
                Documents are optional for submission. Upload now for faster processing and cleaner
                review trails.
              </p>
            </div>

            <div className="document-status-row">
              <span className={`doc-pill ${uploadedDocuments.PAN ? 'uploaded' : ''}`}>
                {uploadedDocuments.PAN ? '✅ PAN uploaded' : 'PAN pending'}
              </span>
              <span className={`doc-pill ${uploadedDocuments.AADHAAR ? 'uploaded' : ''}`}>
                {uploadedDocuments.AADHAAR ? '✅ Aadhaar uploaded' : 'Aadhaar pending'}
              </span>
            </div>
          </div>

          {/* Form Actions */}
          <div className="form-actions">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={handleSaveDraft}
              disabled={loading}
            >
              ← Save Draft
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={loading}
            >
              {loading ? 'Submitting...' : 'Submit Application →'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default BorrowerApplyForLoanPage;
