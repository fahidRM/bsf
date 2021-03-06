//----------------------------------------------------------------------------
// Copyright (C) 2003  Rafael H. Bordini, Jomi F. Hubner, et al.
// 
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
// 
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
// 
// To contact the authors:
// http://www.inf.ufrgs.br/~bordini
// http://www.das.ufsc.br/~jomi
//
// 20130627 jeehanglee@gmail.com	Add properties (duration, priority, deadline)	
//----------------------------------------------------------------------------


package jason.asSemantics;

import jason.asSyntax.ASSyntax;
import jason.asSyntax.Plan;
import jason.asSyntax.PlanBody;
import jason.asSyntax.PlanBodyImpl;
import jason.asSyntax.Pred;
import jason.asSyntax.Term;
import jason.asSyntax.Trigger;

import java.io.Serializable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class IntendedMeans implements Serializable {

    private static final long serialVersionUID = 1L;

    protected Unifier  unif = null;
    protected PlanBody planBody;
    protected Plan     plan;
    private   Trigger  trigger; // the trigger which created this IM
    
    private long deadline = Long.MAX_VALUE;
    private long execStart = Long.MAX_VALUE;
    private long priority;
    private long duration;
    
    public IntendedMeans(Option opt, Trigger te) {
        plan     = opt.getPlan();
        planBody = plan.getBody(); 
        unif     = opt.getUnifier();
        
        if (te == null) {
            trigger = plan.getTrigger().clone();
        } else {
            trigger = te;
        }
        
        // extract priority and deadline info (jeehanglee@gmail.com)
        setSchedulingInfo();
        trigger.apply(unif);
    }
    
    // used by clone
    private IntendedMeans() {  }
    
    // extract priority and deadline from TEvent if exists.
    // otherwise, inherit those from Plan (jeehanglee@gmail.com)
    private void setSchedulingInfo() {
    	long dl = plan.getDeadline(); 
    	long pr = plan.getPriority();
    	
    	Term t = (Term) trigger.getLiteral();
    	if (t != null && t.isPred()) {
    		Pred pred = (Pred) t;
    		if (pred.hasAnnot("deadline"))
    			dl = (long) pred.getAnnotsVal("deadline");
    		if (pred.hasAnnot("priority"))
    			pr = (long) pred.getAnnotsVal("priority"); 
    	}
    	
    	if (dl != Long.MAX_VALUE)
    		deadline = System.currentTimeMillis() + dl;
    	priority = (long) pr;
    	duration = plan.getDuration();
    }
    
    /** removes the current action of the IM and returns the term of the body */
    public Term removeCurrentStep() {
        if (isFinished()) {
            return null;
        } else {
            Term r = planBody.getBodyTerm();
            planBody = planBody.getBodyNext();
            return r;
        }
    }

    public PlanBody getCurrentStep() {
        return planBody;
    }

    // used by if/for/loop internal actions
    public PlanBody insertAsNextStep(PlanBody pb2add) {
        planBody = new PlanBodyImpl(planBody.getBodyType(), planBody.getBodyTerm());
        planBody.setBodyNext(pb2add);
        return planBody; 
    }
    
    public Plan getPlan() {
        return plan;
    }
    
    public void setUnif(Unifier unif) {
        this.unif = unif;
    }
    
    public Unifier getUnif() {
        return unif;
    }

    /** gets the trigger event that caused the creation of this IM */
    public Trigger getTrigger() {
        return trigger;
    }
    public void setTrigger(Trigger tr) {
        trigger = tr;
    }
    
    public long getDeadline() {
    	return deadline;
    }
    
    public long getPriority() {
    	return priority;
    }
    
    public long getDuration() {
    	return duration;
    }
    
    public void setExecStartTime(long time) {
    	execStart = time;
    }
    
    public long getExecuStartTime() {
    	return execStart;
    }

    public boolean isAtomic() {
        return plan != null && plan.isAtomic();
    }
    
    public boolean isFinished() {
        return planBody == null || planBody.isEmptyBody();
    }
    
    public boolean isGoalAdd() {
        return trigger.isAddition() && trigger.isGoal();
    }

    public Object clone() {
        IntendedMeans c = new IntendedMeans();
        c.unif     = this.unif.clone();
        if (this.planBody != null)
            c.planBody = this.planBody.clonePB();
        c.trigger  = this.trigger.clone(); 
        c.plan     = this.plan;
        
        c.deadline = this.deadline;
        c.priority = this.priority;
        c.execStart = this.execStart;
        c.duration 	= this.duration;
        
        return c;
    }
    
    public String toString() {
        return planBody + " / " + unif;
    }

    public Term getAsTerm() {
        if (planBody instanceof PlanBodyImpl) {
            // TODO: use same replacements (Var -> Unnamed var) for the plan and for the unifier
            PlanBody bd = (PlanBody)((PlanBodyImpl)planBody.clone()).makeVarsAnnon();
            bd.setAsBodyTerm(true);
            return ASSyntax.createStructure("im", ASSyntax.createString(plan.getLabel()), bd, unif.getAsTerm());
        } else {
            return ASSyntax.createAtom("noimplementedforclass"+planBody.getClass().getSimpleName());
        }
    }
    
    /** get as XML */
    public Element getAsDOM(Document document) {
        Element eim = (Element) document.createElement("intended-means");
        eim.setAttribute("trigger", trigger.toString());
        if (planBody != null) {
            eim.appendChild(planBody.getAsDOM(document));
        }
        if (unif != null && unif.size() > 0) {
            eim.appendChild(unif.getAsDOM(document));
        }
        return eim;
    }
}
