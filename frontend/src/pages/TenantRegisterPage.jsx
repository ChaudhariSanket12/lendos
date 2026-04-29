import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { authApi } from '../api/identity'

export default function TenantRegisterPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({
    name: '',
    contactEmail: '',
    adminFullName: '',
    adminPassword: '',
  })
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState(false)
  const [loading, setLoading] = useState(false)

  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value })

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError(null)
    try {
      await authApi.registerTenant(form)
      setSuccess(true)
      setTimeout(() => navigate('/login'), 2000)
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12">
      <div className="max-w-md w-full bg-white rounded-lg shadow-md p-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-1">Register Organization</h1>
        <p className="text-gray-500 text-sm mb-6">
          Create your LendOS workspace. The first user becomes the Admin.
        </p>

        {success && (
          <div className="alert-success mb-4">
            Organization registered! Redirecting to login...
          </div>
        )}

        {error && (
          <div className="alert-error mb-4">{error}</div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="label">Organization Name</label>
            <input
              className="input"
              type="text"
              required
              placeholder="e.g. Sharma & Associates CA Firm"
              value={form.name}
              onChange={set('name')}
            />
          </div>

          <div>
            <label className="label">Contact Email</label>
            <input
              className="input"
              type="email"
              required
              placeholder="admin@yourfirm.com"
              value={form.contactEmail}
              onChange={set('contactEmail')}
            />
          </div>

          <div>
            <label className="label">Your Full Name</label>
            <input
              className="input"
              type="text"
              required
              placeholder="Rajesh Sharma"
              value={form.adminFullName}
              onChange={set('adminFullName')}
            />
          </div>

          <div>
            <label className="label">Password</label>
            <input
              className="input"
              type="password"
              required
              minLength={8}
              placeholder="Minimum 8 characters"
              value={form.adminPassword}
              onChange={set('adminPassword')}
            />
          </div>

          <button
            type="submit"
            disabled={loading || success}
            className="btn-primary w-full"
          >
            {loading ? 'Creating workspace...' : 'Create Workspace'}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-gray-500">
          Already registered?{' '}
          <Link to="/login" className="text-blue-600 hover:underline">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  )
}
